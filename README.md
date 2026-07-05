# control-flex-api

Public API for building ControlFlex bridge mods — read controller input, query action states, push mod states, and export compat configurations.

## Installation

```groovy
repositories {
    mavenLocal() // or your preferred Maven repository
}

dependencies {
    compileOnly 'com.ifels.controlflex:controlflex-api:0.8.5'
}
```

## Quick Start

```java
import com.ifels.controlflex.api.*;

// Always check availability first
if (!ControlFlexApi.isAvailable()) return;

// Read controller input
IInputProvider input = ControlFlexApi.getInputProvider();
if (input.isConnected()) {
    IControllerState state = input.getControllerState();
    float moveX = state.getLeftStickX();
    boolean jump = state.isButtonPressed(ButtonName.A);
}

// Query action states
IActionStateProvider actions = ControlFlexApi.getActionStateProvider();
if (actions.isGameActionActive("attack")) {
    // Handle attack
}

// Push mod state
IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
states.setState("mymod:combat_mode", true);
```

## Documentation

See the [ControlFlex API documentation](https://github.com/ControlFlexMC/ControlFlex) for full API reference and developer guides.

## License

MIT
