# Getting Started

## 1. Project Setup

### build.gradle

```groovy
plugins {
    id 'net.neoforged.moddev.legacyforge'  // Forge
    // or 'fabric-loom'                      // Fabric
}

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // ControlFlex API — compileOnly; provided by ControlFlex at runtime
    compileOnly 'com.github.ControlFlexMC:control-flex-api:0.8.7'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

### mods.toml (Forge)

```toml
[[dependencies.your_mod_id]]
    modId = "controlflex"
    mandatory = true
    versionRange = "[0.8.7,)"
    ordering = "AFTER"
    side = "CLIENT"
```

### fabric.mod.json (Fabric)

```json
{
  "depends": {
    "controlflex": ">=0.8.7"
  }
}
```

## 2. Availability Check

**Always check API availability first.** ControlFlex may not be installed or SDL3 may have failed to initialize.

```java
if (!ControlFlexApi.isAvailable()) {
    // Fallback: don't do any controller-related processing
    return;
}
```

## 3. API Overview

```
ControlFlexApi (static entry point)
├── getActionStateProvider()            → IActionStateProvider         Query action states
├── getInputProvider()                  → IInputProvider               Read controller input
│                                          ├── getControllerState()    → IControllerState
│                                          └── getCapabilities()       → IControllerCapabilities
├── getPlayerStateRegistry()            → IPlayerStateRegistry         Push mod states
├── getInputInjector()                  → IInputInjector               Inject virtual input
├── getInteractiveContextRegistrar()    → IInteractiveContextRegistrar Overlay foreground/background
└── isAvailable() / isControllerConnected() / getApiVersion() / reloadGuides()
```

## 4. Two Adaptation Approaches

| | Compat JSON | Bridge Mod (Java) |
|---|---|---|
| **How** | Write JSON config | Implement IControlFlexPlugin (SPI) |
| **Capability** | Key classification, event control, conditional suppression | Full control: input reading, state sync |
| **Use when** | Mod keys are registered via MC KeyMapping | Mod has its own input system |
| **Example** | JEI, MineMenu | EpicFight (combat mode state) |

## 5. Your First Bridge Mod

Create an SPI plugin implementation:

```java
package com.example.mymod;

import com.ifels.controlflex.api.*;

public class MyPlugin implements IControlFlexPlugin {

    @Override
    public String getModId() {
        return "my_bridge_mod";
    }

    @Override
    public void onControlFlexReady() {
        if (!requireApiVersion("0.8.7")) return;
        if (!ControlFlexApi.isAvailable()) return;

        // Initialize your bridge logic
        IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
        states.setState("mymod:initialized", true);
    }
}
```

SPI registration file: `src/main/resources/META-INF/services/com.ifels.controlflex.api.IControlFlexPlugin`

```
com.example.mymod.MyPlugin
```

That's it — no manual registration. ControlFlex discovers plugins automatically.

## 6. Next Steps

- [Plugin Guide](plugin-guide.md) — Full lifecycle, asset installation
- [API Reference](api-reference.md) — Complete interface documentation
- [Compat JSON Config](compat-config.md) — Adapt mods without code
- [Examples](examples.md) — EpicFight bridge and more
