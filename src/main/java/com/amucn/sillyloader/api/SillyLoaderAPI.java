package com.amucn.sillyloader.api;

import com.amucn.sillyloader.SillyLoaderMod;
import com.amucn.sillyloader.loader.SillyModClassLoader;
import com.amucn.sillyloader.loader.SillyModContainer;

import java.util.List;
import java.util.Set;

/**
 * 供其它模组查询傻傻模组加载情况的只读接口。
 */
public final class SillyLoaderAPI {

    private SillyLoaderAPI() {
    }

    public static int getLoadedSillyModCount() {
        SillyModClassLoader loader = SillyLoaderMod.getModLoader();
        return loader == null ? 0 : loader.getLoadedModCount();
    }

    public static List<SillyModContainer> getAllSillyMods() {
        SillyModClassLoader loader = SillyLoaderMod.getModLoader();
        return loader == null ? List.of() : loader.getLoadedMods();
    }

    public static Set<String> getLoadedSillyModNames() {
        SillyModClassLoader loader = SillyLoaderMod.getModLoader();
        return loader == null ? Set.of() : loader.getLoadedModNames();
    }

    public static boolean isSillyModLoaded(String modName) {
        SillyModClassLoader loader = SillyLoaderMod.getModLoader();
        return loader != null && loader.isModLoaded(modName);
    }

    public static String getLoaderVersion() {
        return SillyLoaderMod.VERSION;
    }
}
