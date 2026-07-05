# Plugin 开发指南

## IControlFlexPlugin 接口

```java
public interface IControlFlexPlugin {

    /** 桥接模组的 Mod ID */
    String getModId();

    /** 导出 compat JSON 到 cfx-mod/ */
    default void onInstallCompatConfigs(ICompatAssetInstaller installer) {}

    /** 导出 guide JSON 到 cfx-mod/ */
    default void onInstallGuideAssets(ICompatAssetInstaller installer) {}

    /** ControlFlex 完全就绪 */
    default void onControlFlexReady() {}

    /** 控制器连接状态变化 */
    default void onControllerConnectionChanged(boolean connected) {}

    /** 版本检查便利方法 */
    default boolean requireApiVersion(String minVersion) { ... }
}
```

## 生命周期回调

### onInstallCompatConfigs

在 `onControlFlexReady` 之前调用。用 `ICompatAssetInstaller.install()` 安装 compat JSON。

目标目录: `config/controlflex/compat/cfx-mod/`  
优先级: 高于 `default/`，低于 `user/`  
覆盖规则: 同 mod_id 完全覆盖

```java
@Override
public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
    installer.install(
        "/assets/mymod/compat/targetmod_keys.json",
        "targetmod_keys.json"
    );
}
```

### onInstallGuideAssets

同上，但安装 guide JSON 到 `config/controlflex/guides/cfx-mod/`。

```java
@Override
public void onInstallGuideAssets(ICompatAssetInstaller installer) {
    installer.install(
        "/assets/mymod/guides/targetmod_guid.json",
        "targetmod_guid.json"
    );
}
```

### onControlFlexReady

**唯一的初始化回调**。此时所有 API provider 已就绪。

```java
@Override
public void onControlFlexReady() {
    // 1. 版本检查
    if (!requireApiVersion("0.8.5")) return;

    // 2. API 可用性
    if (!ControlFlexApi.isAvailable()) return;

    // 3. 注册事件监听器
    MinecraftForge.EVENT_BUS.addListener(this::onTargetModEvent);

    // 4. 推送初始状态
    ControlFlexApi.getPlayerStateRegistry()
        .setState("mymod:initialized", true);
}
```

### onControllerConnectionChanged

控制器插拔时调用。

```java
@Override
public void onControllerConnectionChanged(boolean connected) {
    if (connected) {
        String name = ControlFlexApi.getInputProvider().getGamepadName();
        LOGGER.info("控制器已连接: {}", name);
    } else {
        LOGGER.info("控制器已断开");
        resetState();
    }
}
```

## SPI 注册

创建文件: `src/main/resources/META-INF/services/com.ifels.controlflex.api.IControlFlexPlugin`

```
com.example.mymod.MyPlugin
```

## 目录分层

ControlFlex 使用三层目录管理配置:

```
config/controlflex/compat/          config/controlflex/guides/
├── default/    ← ControlFlex 内置    ├── default/
├── cfx-mod/    ← 桥接模组安装         ├── cfx-mod/
└── user/       ← 用户自定义            └── user/
```

优先级: `user > cfx-mod > default`

桥接模组的 `onInstallCompatConfigs` / `onInstallGuideAssets` 写入 `cfx-mod/`。

## 线程安全

- **所有 API 调用必须在客户端主线程**（Minecraft 游戏循环线程）
- 例外: `IPlayerStateRegistry.setState()` 可从任意线程调用
- `IControllerState` 是实时视图，**不要跨 tick 缓存引用**
- `getActiveGameActions()` 返回的 Set 是实时视图，**不要跨 tick 缓存**

## Logging

用你的 Mod 自己的 Logger:

```java
private static final Logger LOGGER = LogManager.getLogger("my-mod-id");
```

## 完整模板

```java
package com.example.mymod;

import com.ifels.controlflex.api.*;
import org.apache.logging.log4j.*;

public class MyPlugin implements IControlFlexPlugin {

    private static final Logger LOGGER = LogManager.getLogger("my-bridge-mod");

    @Override
    public String getModId() {
        return "my_bridge_mod";
    }

    @Override
    public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
        installer.install(
            "/assets/my_bridge_mod/compat/targetmod_keys.json",
            "targetmod_keys.json");
    }

    @Override
    public void onInstallGuideAssets(ICompatAssetInstaller installer) {
        installer.install(
            "/assets/my_bridge_mod/guides/targetmod_guid.json",
            "targetmod_guid.json");
    }

    @Override
    public void onControlFlexReady() {
        if (!requireApiVersion("0.8.5")) return;
        if (!ControlFlexApi.isAvailable()) return;

        IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
        states.setState("targetmod:initialized", true);

        LOGGER.info("Bridge mod ready. API version: {}",
            ControlFlexApi.getApiVersion());
    }

    @Override
    public void onControllerConnectionChanged(boolean connected) {
        LOGGER.info("Controller connection: {}", connected);
    }
}
```
