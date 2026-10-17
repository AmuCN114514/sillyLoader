package com.amucn.sillyloader;

import com.amucn.sillyloader.loader.SillyModClassLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mod(SillyLoaderMod.MOD_ID)
public class SillyLoaderMod {
    public static final String MOD_ID = "sillyloader";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final String VERSION = "0.5.2-beta";

    /** 二级模组所在目录，相对于游戏工作目录。 */
    public static final String SILLY_MODS_DIR = "sillymods";

    private static SillyModClassLoader modLoader;

    public SillyLoaderMod(IEventBus modEventBus) {
        LOGGER.info("sillyLoader v{} 初始化中...", VERSION);

        modEventBus.addListener(this::commonSetup);
        Runtime.getRuntime().addShutdownHook(new Thread(SillyLoaderMod::shutdownLoader, "sillyLoader-shutdown"));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("sillyLoader 正在加载傻傻模组...");

        event.enqueueWork(() -> {
            Path modsDir = Paths.get(".", SILLY_MODS_DIR);
            modLoader = new SillyModClassLoader(modsDir);
            modLoader.loadAllSillyMods();
        });
    }

    /**
     * 关闭加载器并释放所有二级模组的类加载器。
     * 由 JVM shutdown hook 触发。
     */
    public static synchronized void shutdownLoader() {
        if (modLoader == null) {
            return;
        }
        modLoader.shutdown();
        modLoader = null;
    }

    public static SillyModClassLoader getModLoader() {
        return modLoader;
    }
}
