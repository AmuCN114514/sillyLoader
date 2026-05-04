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
    
    private static SillyModClassLoader modLoader;

    public SillyLoaderMod(IEventBus modEventBus) {
        LOGGER.info("sillyLoader v{} initializing... (内测版，含有大量bug)", VERSION);
        
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("sillyLoader 正在加载傻傻模组...");
        
        event.enqueueWork(() -> {
            Path modsDir = Paths.get(".", "sillymods");
            modLoader = new SillyModClassLoader(modsDir);
            modLoader.loadAllSillyMods();
            
            LOGGER.info("已加载 {} 个傻傻模组", modLoader.getLoadedModCount());
        });
    }
    
    public static SillyModClassLoader getModLoader() {
        return modLoader;
    }
}
