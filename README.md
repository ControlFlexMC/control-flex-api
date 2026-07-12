# control-flex-api

Public API for building ControlFlex bridge mods — read controller input, query action states, push mod states, and export compat configurations.

[中文文档](README_ZH.md)

## Documentation

| Document | Content |
|----------|---------|
| [Getting Started](docs/en/getting-started.md) | Project setup, API overview, first plugin |
| [API Reference](docs/en/api-reference.md) | Complete interface signatures and descriptions |
| [Plugin Guide](docs/en/plugin-guide.md) | Lifecycle, asset installation, full template |
| [Compat JSON Config](docs/en/compat-config.md) | Adapt mod keys without writing code |
| [Examples](docs/en/examples.md) | EpicFight bridge and other end-to-end code |

## Quick Start

```groovy
// build.gradle
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    compileOnly 'com.github.ControlFlexMC:control-flex-api:0.9.0'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

```java
import com.ifels.controlflex.api.*;

// Always check availability
if (!ControlFlexApi.isAvailable()) return;

// Read controller input
IInputProvider input = ControlFlexApi.getInputProvider();
if (input.isConnected()) {
    IControllerState state = input.getControllerState();
    float moveX = state.getLeftStickX();
    boolean jump = state.isButtonPressed(ButtonName.A);
}

// Push mod state
IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
states.setState("mymod:combat_mode", true);
```

## License

MIT
