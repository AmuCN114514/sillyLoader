package com.amucn.sillyloader.loader;

import com.amucn.sillyloader.SillyLoaderMod;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SillyModClassLoader {
    private final Path modsDirectory;
    private final List<SillyModContainer> loadedMods = new ArrayList<>();
    private final List<URLClassLoader> classLoaders = new ArrayList<>();
    
    public SillyModClassLoader(Path modsDirectory) {
        this.modsDirectory = modsDirectory;
    }
    
    public void loadAllSillyMods() {
        if (!Files.exists(modsDirectory)) {
            try {
                Files.createDirectories(modsDirectory);
                SillyLoaderMod.LOGGER.info("已创建 sillymods 文件夹: {}", modsDirectory.toAbsolutePath());
                return;
            } catch (IOException e) {
                SillyLoaderMod.LOGGER.error("无法创建 sillymods 文件夹", e);
                return;
            }
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDirectory, "*.jar")) {
            for (Path jarPath : stream) {
                loadSillyMod(jarPath);
            }
        } catch (IOException e) {
            SillyLoaderMod.LOGGER.error("扫描 sillymods 文件夹时出错", e);
        }
    }
    
    private void loadSillyMod(Path jarPath) {
        try {
            URL jarUrl = jarPath.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
            classLoaders.add(classLoader);
            
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                List<Class<?>> annotatedClasses = new ArrayList<>();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    if (name.endsWith(".class") && !name.contains("module-info")) {
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            if (clazz.isAnnotationPresent(SillyMod.class)) {
                                annotatedClasses.add(clazz);
                                SillyMod annotation = clazz.getAnnotation(SillyMod.class);
                                SillyLoaderMod.LOGGER.info("发现傻傻模组: {} v{}", annotation.name(), annotation.version());
                            }
                        } catch (ClassNotFoundException e) {
                            SillyLoaderMod.LOGGER.warn("无法加载类: {}", className, e);
                        }
                    }
                }
                
                if (!annotatedClasses.isEmpty()) {
                    SillyModContainer container = new SillyModContainer(jarPath, annotatedClasses, classLoader);
                    loadedMods.add(container);
                    initializeMod(container);
                }
            }
        } catch (Exception e) {
            SillyLoaderMod.LOGGER.error("加载傻傻模组失败: {}", jarPath, e);
        }
    }
    
    private void initializeMod(SillyModContainer container) {
        for (Class<?> clazz : container.getModClasses()) {
            try {
                SillyMod annotation = clazz.getAnnotation(SillyMod.class);
                Object instance = clazz.getDeclaredConstructor().newInstance();
                container.addInstance(instance);
                
                if (instance instanceof SillyModInitializer) {
                    ((SillyModInitializer) instance).onInitialize();
                }
                
                SillyLoaderMod.LOGGER.info("已初始化傻傻模组: {}", annotation.name());
            } catch (Exception e) {
                SillyLoaderMod.LOGGER.error("初始化模组失败: {}", clazz.getName(), e);
            }
        }
    }
    
    public int getLoadedModCount() {
        return loadedMods.size();
    }
    
    public List<SillyModContainer> getLoadedMods() {
        return Collections.unmodifiableList(loadedMods);
    }
    
    public void shutdown() {
        for (URLClassLoader classLoader : classLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                SillyLoaderMod.LOGGER.warn("关闭类加载器失败", e);
            }
        }
        classLoaders.clear();
        loadedMods.clear();
    }
    
    public interface SillyModInitializer {
        void onInitialize();
    }
}
