# API Reference

> Package: `com.ifels.controlflex.api`  
> Version: 0.8.5  
> Thread model: All methods must be called from the client thread unless noted otherwise

## Type List

| Type | Description |
|------|-------------|
| `ControlFlexApi` | Static entry point, access all providers |
| `ButtonName` | Button/axis name constants |
| `IControlFlexPlugin` | SPI plugin interface |
| `ICompatAssetInstaller` | Asset installer (SPI callback parameter) |
| `IActionStateProvider` | Action state queries |
| `IInputProvider` | Controller input queries |
| `IControllerState` | Controller hardware state (live view) |
| `IControllerCapabilities` | Controller hardware capabilities |
| `IPlayerStateRegistry` | Third-party mod state push |
| `InputMode` | Input mode (KEYBOARD_MOUSE / MIXED) |
| `ControllerType` | Controller type enum |

---

## ControlFlexApi

The single entry point for bridge mods. All methods return safe defaults when ControlFlex is not installed.

```java
public final class ControlFlexApi
```

### Availability

```java
boolean isAvailable()               // ControlFlex installed and fully initialized
boolean isControllerConnected()     // A controller is connected
```

### Provider Access

```java
IActionStateProvider getActionStateProvider()    // null if unavailable
IInputProvider getInputProvider()                // null if unavailable
IPlayerStateRegistry getPlayerStateRegistry()    // null if unavailable
```

### Utilities

```java
String getApiVersion()    // e.g. "0.8.5"
void reloadGuides()       // Reload guide definitions
```

---

## ButtonName

Name constants for `isButtonPressed(String)` and `getAxisValue(String)`.

```java
// Buttons
ButtonName.A, ButtonName.B, ButtonName.X, ButtonName.Y
ButtonName.DPAD_UP, ButtonName.DPAD_DOWN, ButtonName.DPAD_LEFT, ButtonName.DPAD_RIGHT
ButtonName.LEFT_BUMPER, ButtonName.RIGHT_BUMPER
ButtonName.LEFT_TRIGGER, ButtonName.RIGHT_TRIGGER
ButtonName.LEFT_STICK_CLICK, ButtonName.RIGHT_STICK_CLICK
ButtonName.BACK, ButtonName.START, ButtonName.GUIDE
ButtonName.PADDLE_1 ~ ButtonName.PADDLE_4
ButtonName.TOUCHPAD

// Axes (for getAxisValue)
ButtonName.AXIS_LEFT_X, ButtonName.AXIS_LEFT_Y
ButtonName.AXIS_RIGHT_X, ButtonName.AXIS_RIGHT_Y
ButtonName.AXIS_LEFT_TRIGGER, ButtonName.AXIS_RIGHT_TRIGGER
```

---

## IControlFlexPlugin

SPI interface for bridge mods. Registered via `META-INF/services/`.

```java
String getModId()

// Lifecycle callbacks (in call order):
void onInstallCompatConfigs(ICompatAssetInstaller installer)   // Export compat JSON
void onInstallGuideAssets(ICompatAssetInstaller installer)     // Export guide JSON
void onControlFlexReady()                                       // ControlFlex is ready
void onControllerConnectionChanged(boolean connected)           // Controller plug/unplug

// Utility:
boolean requireApiVersion(String minVersion)  // Version check, returns false if unsatisfied
```

### Lifecycle

```
ControlFlex startup
  └── initializeClient()
        → API providers injected (isAvailable() = true)
        → Built-in configs extracted to default/

  └── onKeyMappingsReady()
        └── SPI plugin discovery
              ├── plugin.onInstallCompatConfigs(installer)
              ├── plugin.onInstallGuideAssets(installer)
              └── plugin.onControlFlexReady()

  Runtime:
        └── plugin.onControllerConnectionChanged(connected)
```

---

## ICompatAssetInstaller

Asset installer provided by ControlFlex for exporting resources from the bridge mod's JAR.

```java
boolean install(String resourcePath, String fileName)
```

```java
@Override
public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
    installer.install("/assets/mymod/compat/epicfight_keys.json", "epicfight_keys.json");
}
```

---

## IActionStateProvider

Query ControlFlex action states.

```java
boolean isGameActionActive(String actionId)
boolean isGuiActionActive(String actionId)
Set<String> getActiveGameActions()   // live view — do not cache across ticks
Set<String> getActiveGuiActions()    // live view — do not cache across ticks
```

**Action ID format**: `"modId:keyName"` (e.g. `"epicfight:key.epicfight.attack"`) or short names like `"attack"`, `"use"`.

---

## IInputProvider

```java
boolean isConnected()
IControllerState getControllerState()          // null if disconnected (live view, don't cache)
String getGamepadName()                        // null if disconnected
int getGamepadIndex()                          // -1 if disconnected
IControllerCapabilities getCapabilities()      // null if disconnected
```

---

## IControllerState

Live view of controller hardware state. Sticks -1.0~1.0, triggers 0.0~1.0. Y-axis positive = down.

```java
// Sticks
float getLeftStickX() / getLeftStickY()
float getRightStickX() / getRightStickY()

// Triggers
float getLeftTrigger() / getRightTrigger()
boolean isLeftTriggerPressed() / isRightTriggerPressed()   // threshold 0.5

// Buttons (all camelCase)
boolean isButtonAPressed() / isButtonBPressed() / isButtonXPressed() / isButtonYPressed()
boolean isButtonBackPressed() / isButtonStartPressed() / isButtonGuidePressed()
boolean isLeftBumperPressed() / isRightBumperPressed()
boolean isLeftStickClicked() / isRightStickClicked()
boolean isDpadUpPressed() / isDpadDownPressed() / isDpadLeftPressed() / isDpadRightPressed()
boolean isPaddle1Pressed() ~ isPaddle4Pressed()
boolean isTouchpadPressed()
boolean isShiftLayerActive()

// String queries — prefer ButtonName constants
boolean isButtonPressed(String buttonName)
float getAxisValue(String axisName)
```

---

## IControllerCapabilities

```java
ControllerType getControllerType()   // XBOX / PLAYSTATION / NINTENDO_SWITCH / ...
boolean hasPaddles()
int getPaddleCount()
boolean hasTouchpad()
boolean hasGyro()
boolean hasRumble()
boolean hasAnalogTriggers()
boolean isNintendoLayout()
```

---

## IPlayerStateRegistry

Bridge mods push states for use in compat JSON `playerState` conditions.

```java
void setState(String stateKey, boolean active)  // thread-safe
boolean getState(String stateKey)               // client thread
void clearState(String stateKey)                // remove registration
```

**stateKey format**: `"modId:stateName"` (e.g. `"epicfight:battle_mode"`)

---

## Thread Model

| Interface | Constraint |
|-----------|-----------|
| `ControlFlexApi` | Client main thread |
| `IActionStateProvider` | Client main thread |
| `IInputProvider` / `IControllerState` | Client main thread (volatile-backed fields) |
| `IControllerCapabilities` | Client main thread |
| `IPlayerStateRegistry.setState()` | **Any thread** |
| `IPlayerStateRegistry.getState()` | Client main thread |
