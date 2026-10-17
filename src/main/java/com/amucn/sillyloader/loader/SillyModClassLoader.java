package com.amucn.sillyloader.loader;

import com.amucn.sillyloader.SillyLoaderMod;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 扫描 {@code sillymods} 目录并加载其中的二级模组。
 */
public class SillyModClassLoader {
    private static final int STATE_VISITING = 1;
    private static final int STATE_VISITED = 2;

    private final Path modsDirectory;
    private final List<SillyModContainer> loadedMods = new ArrayList<>();
    private final List<URLClassLoader> classLoaders = new ArrayList<>();
    private final Set<String> loadedModNames = new HashSet<>();
    private boolean closed = false;

    public SillyModClassLoader(Path modsDirectory) {
        this.modsDirectory = modsDirectory;
    }

    /**
     * 扫描目录、解析依赖并按顺序初始化所有傻傻模组。
     */
    public void loadAllSillyMods() {
        if (!Files.exists(modsDirectory)) {
            try {
                Files.createDirectories(modsDirectory);
                SillyLoaderMod.LOGGER.info("未找到 sillymods 文件夹，已创建: {}", modsDirectory.toAbsolutePath());
            } catch (IOException e) {
                SillyLoaderMod.LOGGER.error("无法创建 sillymods 文件夹", e);
            }
            return;
        }

        List<Path> jars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDirectory, "*.jar")) {
            for (Path jarPath : stream) {
                jars.add(jarPath);
            }
        } catch (IOException e) {
            SillyLoaderMod.LOGGER.error("扫描 sillymods 文件夹时出错", e);
            return;
        }
        // 固定加载顺序，保证同名冲突时结果可复现
        jars.sort(Comparator.comparing((Path path) -> path.getFileName().toString()));

        List<PendingMod> pending = new ArrayList<>();
        for (Path jarPath : jars) {
            scanJar(jarPath, pending);
        }

        for (PendingMod mod : resolveLoadOrder(pending)) {
            initializeMod(mod);
        }
        SillyLoaderMod.LOGGER.info("sillymods 扫描完成，共加载 {} 个傻傻模组", getLoadedModCount());
    }

    /**
     * 扫描单个 jar，收集其中所有带 {@link SillyMod} 注解的类。
     */
    private void scanJar(Path jarPath, List<PendingMod> pending) {
        URLClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader());
            List<Class<?>> annotatedClasses = collectAnnotatedClasses(jarPath, classLoader);
            if (annotatedClasses.isEmpty()) {
                closeQuietly(classLoader);
                return;
            }

            SillyModContainer container = new SillyModContainer(jarPath, annotatedClasses, classLoader);
            classLoaders.add(classLoader);
            loadedMods.add(container);

            for (Class<?> clazz : annotatedClasses) {
                SillyMod meta = clazz.getAnnotation(SillyMod.class);
                pending.add(new PendingMod(meta.name(), meta.version(),
                    new LinkedHashSet<>(List.of(meta.dependencies())), clazz, container, jarPath));
            }
        } catch (Exception e) {
            SillyLoaderMod.LOGGER.error("扫描傻傻模组失败: {}", jarPath, e);
            closeQuietly(classLoader);
        }
    }

    private List<Class<?>> collectAnnotatedClasses(Path jarPath, URLClassLoader classLoader) {
        List<Class<?>> annotatedClasses = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (!entryName.endsWith(".class") || entryName.contains("module-info")) {
                    continue;
                }
                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz.isAnnotationPresent(SillyMod.class)) {
                        annotatedClasses.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    SillyLoaderMod.LOGGER.warn("跳过无法加载的类 {}: {}", className, e.toString());
                } catch (LinkageError e) {
                    // 缺少依赖库或字节码版本不兼容时会出现
                    SillyLoaderMod.LOGGER.warn("跳过无法链接的类 {}: {}", className, e.toString());
                }
            }
        } catch (IOException e) {
            SillyLoaderMod.LOGGER.error("读取 jar 失败: {}", jarPath, e);
        }
        return annotatedClasses;
    }

    /**
     * 过滤重名模组，并按 {@code dependencies} 做拓扑排序。
     */
    private List<PendingMod> resolveLoadOrder(List<PendingMod> pending) {
        Map<String, PendingMod> uniqueMods = new LinkedHashMap<>();
        for (PendingMod mod : pending) {
            PendingMod existing = uniqueMods.get(mod.name());
            if (existing != null) {
                SillyLoaderMod.LOGGER.warn("发现重名傻傻模组 {}: {} 将被忽略（已收录 {}）",
                    mod.name(), mod.jarPath().getFileName(), existing.jarPath().getFileName());
                continue;
            }
            uniqueMods.put(mod.name(), mod);
        }
        pruneUnusedContainers(uniqueMods.values());

        Map<String, Integer> state = new HashMap<>();
        List<PendingMod> ordered = new ArrayList<>();
        for (PendingMod mod : uniqueMods.values()) {
            visitDependency(mod, uniqueMods, state, ordered);
        }
        return ordered;
    }

    private void visitDependency(PendingMod mod, Map<String, PendingMod> uniqueMods,
                                 Map<String, Integer> state, List<PendingMod> ordered) {
        Integer current = state.get(mod.name());
        if (current != null) {
            if (current == STATE_VISITING) {
                SillyLoaderMod.LOGGER.warn("傻傻模组 {} 存在循环依赖，按当前顺序继续加载", mod.name());
            }
            return;
        }
        state.put(mod.name(), STATE_VISITING);
        for (String dependency : mod.dependencies()) {
            PendingMod provider = uniqueMods.get(dependency);
            if (provider == null) {
                SillyLoaderMod.LOGGER.error("傻傻模组 {} 缺少依赖 {}，仍将尝试加载", mod.name(), dependency);
                continue;
            }
            visitDependency(provider, uniqueMods, state, ordered);
        }
        state.put(mod.name(), STATE_VISITED);
        ordered.add(mod);
    }

    /** 释放完全没有模组在其中生效的 jar。 */
    private void pruneUnusedContainers(Iterable<PendingMod> usedMods) {
        Set<SillyModContainer> used = new HashSet<>();
        for (PendingMod mod : usedMods) {
            used.add(mod.container());
        }
        for (SillyModContainer container : new ArrayList<>(loadedMods)) {
            if (used.contains(container)) {
                continue;
            }
            loadedMods.remove(container);
            classLoaders.remove(container.getClassLoader());
            closeQuietly(container.getClassLoader());
        }
    }

    private void initializeMod(PendingMod mod) {
        Class<?> modClass = mod.clazz();
        if (modClass.isInterface() || Modifier.isAbstract(modClass.getModifiers())) {
            SillyLoaderMod.LOGGER.error("傻傻模组 {} 的入口类不能是接口或抽象类: {}", mod.name(), modClass.getName());
            return;
        }
        try {
            Constructor<?> constructor = modClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            mod.container().addInstance(instance);

            if (instance instanceof SillyModInitializer initializer) {
                initializer.onInitialize();
                initializer.registerEvents(NeoForge.EVENT_BUS);
            }

            loadedModNames.add(mod.name());
            SillyLoaderMod.LOGGER.info("已初始化傻傻模组: {} v{}", mod.name(), mod.version());
        } catch (Throwable t) {
            // 单个模组出错不应影响其它模组和游戏本身
            SillyLoaderMod.LOGGER.error("初始化傻傻模组 {} 失败", mod.name(), t);
        }
    }

    public int getLoadedModCount() {
        return loadedModNames.size();
    }

    public List<SillyModContainer> getLoadedMods() {
        return Collections.unmodifiableList(loadedMods);
    }

    public Set<String> getLoadedModNames() {
        return Collections.unmodifiableSet(loadedModNames);
    }

    public boolean isModLoaded(String modName) {
        return loadedModNames.contains(modName);
    }

    /**
     * 关闭所有类加载器并清空加载状态，由 {@code SillyLoaderMod} 在退出时调用。
     */
    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        for (URLClassLoader classLoader : classLoaders) {
            closeQuietly(classLoader);
        }
        classLoaders.clear();
        loadedMods.clear();
        loadedModNames.clear();
        SillyLoaderMod.LOGGER.info("sillyLoader 已关闭所有傻傻模组类加载器");
    }

    private static void closeQuietly(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException e) {
            SillyLoaderMod.LOGGER.warn("关闭类加载器失败", e);
        }
    }

    /** 已扫描但尚未初始化的模组。 */
    private record PendingMod(String name, String version, Set<String> dependencies,
                              Class<?> clazz, SillyModContainer container, Path jarPath) {
    }
}
