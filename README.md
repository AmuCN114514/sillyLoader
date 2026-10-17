# sillyLoader

一个 **NeoForge 二级模组加载器**：把带有 `@SillyMod` 注解的 jar 丢进游戏目录下的 `./sillymods/` 文件夹，就能在启动时动态加载，无需重新构建整合包。

> 当前版本 `0.5.2-beta`，仍在开发中，对外接口可能变动。

## 特性

- **目录扫描**：自动创建并扫描 `./sillymods/` 下的所有 `*.jar`
- **独立类加载**：每个 jar 使用独立的 `URLClassLoader`，互不干扰
- **依赖解析**：按 `@SillyMod#dependencies` 做拓扑排序，缺失依赖会明确报错
- **循环依赖检测**：检测到环时告警并按当前顺序继续，避免死循环
- **重名过滤**：同名模组只保留先扫描到的一个，并提示冲突来源
- **事件总线**：实现 `SillyModInitializer` 的模组可拿到 `NeoForge.EVENT_BUS` 注册监听器
- **优雅退出**：游戏退出时通过 JVM shutdown hook 关闭所有类加载器
- **容错**：单个模组加载或初始化失败不会影响其它模组与游戏本身

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft / NeoForge | NeoForge `26.1.2`（依赖声明 `[26,)`） |
| Java | 21 |
| 构建 | Gradle Wrapper `8.14.5` + `net.neoforged.gradle.userdev` `7.0.158` |

## 安装

1. 构建本模组：`./gradlew build`，产物在 `build/libs/`；
2. 把 `sillyloader-<version>.jar` 放进整合包的 `mods/` 文件夹；
3. 启动一次游戏，工作目录下会生成 `./sillymods/` 文件夹；
4. 把写好的傻傻模组 jar 放进去，重启游戏即可。

## 编写一个傻傻模组

入口类需要带 `@SillyMod` 注解、有可用的无参构造器，可选实现 `SillyModInitializer`：

```java
package com.example.mymod;

import com.amucn.sillyloader.loader.SillyMod;
import com.amucn.sillyloader.loader.SillyModInitializer;
import net.neoforged.bus.api.IEventBus;

@SillyMod(
    name = "my-silly-mod",
    version = "1.0.0",
    description = "一个示例傻傻模组",
    dependencies = {"another-silly-mod"}
)
public class MySillyMod implements SillyModInitializer {

    @Override
    public void onInitialize() {
        // 模组加载时调用
    }

    @Override
    public void registerEvents(IEventBus eventBus) {
        // 可选：注册 NeoForge 游戏事件监听器
    }
}
```

编译出 jar 后放进 `./sillymods/` 即可。

### `@SillyMod` 注解参数

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `name` | `String` | 必填 | 模组唯一名称，依赖解析与重名检测都基于它 |
| `version` | `String` | `"1.0.0"` | 版本号 |
| `description` | `String` | `""` | 模组描述 |
| `dependencies` | `String[]` | `{}` | 依赖的其它傻傻模组名称 |

## 目录结构

```
src/main/java/com/amucn/
├── SillyLoaderMod.java                    # NeoForge @Mod 入口，注册扫描与退出钩子
└── sillyloader/
    ├── api/SillyLoaderAPI.java            # 对外查询接口
    └── loader/
        ├── SillyMod.java                  # @SillyMod 注解
        ├── SillyModContainer.java         # 单个 jar 的封装
        ├── SillyModInitializer.java       # 模组初始化 / 事件注册接口
        └── SillyModClassLoader.java       # 扫描、依赖解析、加载的核心
```

## API

```java
SillyLoaderAPI.getLoadedSillyModCount();   // 已加载的傻傻模组数量
SillyLoaderAPI.getAllSillyMods();          // 所有已加载的封装对象
SillyLoaderAPI.getLoadedSillyModNames();   // 所有已加载的模组名
SillyLoaderAPI.isSillyModLoaded("xxx");    // 某个模组是否已加载
SillyLoaderAPI.getLoaderVersion();         // 加载器版本
```

## 构建

仓库自带 Gradle Wrapper（Gradle 8.14.5）：

```bash
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

首次执行会下载 Gradle 发行包，完成后产物位于 `build/libs/`。

## 注意事项

- 加载进来的模组**不会**被注册成 NeoForge 模组，只在 `onInitialize()` 与事件监听器里活动；
- 傻傻模组 jar 引用的第三方库需自行提供，否则相关类会被跳过并输出告警；
- 版本号需在 `gradle.properties`、`neoforge.mods.toml` 与 `SillyLoaderMod.VERSION` 三处保持一致。

## 许可证

[MIT](LICENSE)
