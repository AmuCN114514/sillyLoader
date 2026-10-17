package com.amucn.sillyloader.loader;

import net.neoforged.bus.api.IEventBus;

/**
 * 傻傻模组的可选生命周期接口。
 * 入口类实现该接口后，加载器会在初始化时回调下面的方法。
 */
public interface SillyModInitializer {
    /**
     * 模组被加载时调用，可在此做初始化。
     */
    void onInitialize();

    /**
     * 可选：把监听器注册到 NeoForge 游戏事件总线。
     *
     * @param eventBus NeoForge 全局事件总线（NeoForge.EVENT_BUS）
     */
    default void registerEvents(IEventBus eventBus) {
    }
}
