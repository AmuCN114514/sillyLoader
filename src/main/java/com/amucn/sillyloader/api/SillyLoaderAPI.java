package com.amucn.sillyloader.api;

import com.amucn.sillyloader.SillyLoaderMod;
import com.amucn.sillyloader.loader.SillyModContainer;
import java.util.List;

public class SillyLoaderAPI {
    
    public static int getLoadedSillyModCount() {
        if (SillyLoaderMod.getModLoader() == null) {
            return 0;
        }
        return SillyLoaderMod.getModLoader().getLoadedModCount();
    }
    
    public static List<SillyModContainer> getAllSillyMods() {
        if (SillyLoaderMod.getModLoader() == null) {
            return List.of();
        }
        return SillyLoaderMod.getModLoader().getLoadedMods();
    }
    
    public static boolean isSillyModLoaded(String modName) {
        return getAllSillyMods().stream()
            .flatMap(container -> container.getModClasses().stream())
            .anyMatch(clazz -> {
                var annotation = clazz.getAnnotation(com.amucn.sillyloader.loader.SillyMod.class);
                return annotation != null && annotation.name().equals(modName);
            });
    }
    
    public static String getLoaderVersion() {
        return SillyLoaderMod.VERSION;
    }
}
