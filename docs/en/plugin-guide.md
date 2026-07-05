# Plugin Development Guide

## IControlFlexPlugin Interface

```java
public interface IControlFlexPlugin {

    String getModId();

    default void onInstallCompatConfigs(ICompatAssetInstaller installer) {}
    default void onInstallGuideAssets(ICompatAssetInstaller installer) {}
    default void onControlFlexReady() {}
    default void onControllerConnectionChanged(boolean connected) {}
    default boolean requireApiVersion(String minVersion) { ... }
}
```

## Lifecycle Callbacks

### onInstallCompatConfigs

Called before `onControlFlexReady`. Use `ICompatAssetInstaller.install()` to export compat JSON files. Target: `config/controlflex/compat/cfx-mod/`. Priority: above `default/`, below `user/`. Same mod_id = override.

```java
@Override
public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
    installer.install(
        "/assets/mymod/compat/targetmod_keys.json",
        "targetmod_keys.json");
}
```

### onInstallGuideAssets

Same pattern for guide JSON. Target: `config/controlflex/guides/cfx-mod/`.

### onControlFlexReady

The single initialization callback. All API providers are ready.

```java
@Override
public void onControlFlexReady() {
    if (!requireApiVersion("0.8.5")) return;
    if (!ControlFlexApi.isAvailable()) return;

    // Register event listeners
    MinecraftForge.EVENT_BUS.addListener(this::onTargetModEvent);

    // Push initial state
    ControlFlexApi.getPlayerStateRegistry()
        .setState("mymod:initialized", true);
}
```

### onControllerConnectionChanged

Called when a controller is connected or disconnected.

```java
@Override
public void onControllerConnectionChanged(boolean connected) {
    if (connected) {
        String name = ControlFlexApi.getInputProvider().getGamepadName();
        LOGGER.info("Controller connected: {}", name);
    }
}
```

## SPI Registration

Create: `src/main/resources/META-INF/services/com.ifels.controlflex.api.IControlFlexPlugin`

```
com.example.mymod.MyPlugin
```

## Directory Layering

```
config/controlflex/compat/          config/controlflex/guides/
├── default/    ← ControlFlex built-in   ├── default/
├── cfx-mod/    ← Bridge mod installs     ├── cfx-mod/
└── user/       ← User customizations     └── user/
```

Priority: `user > cfx-mod > default`

## Thread Safety

- **All API calls must be on the client main thread** (Minecraft game loop)
- Exception: `IPlayerStateRegistry.setState()` can be called from any thread
- `IControllerState` is a live view — **do not cache across ticks**
- `getActiveGameActions()` returns a live set — **do not cache across ticks**

## Complete Template

```java
package com.example.mymod;

import com.ifels.controlflex.api.*;
import org.apache.logging.log4j.*;

public class MyPlugin implements IControlFlexPlugin {

    private static final Logger LOGGER = LogManager.getLogger("my-bridge-mod");

    @Override
    public String getModId() { return "my_bridge_mod"; }

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
        LOGGER.info("Bridge mod ready. API: {}", ControlFlexApi.getApiVersion());
    }

    @Override
    public void onControllerConnectionChanged(boolean connected) {
        LOGGER.info("Controller: {}", connected);
    }
}
```
