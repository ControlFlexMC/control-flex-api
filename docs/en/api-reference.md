# API Reference

> Package: `com.ifels.controlflex.api`  
> Version: 0.8.6  
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
| `IInputInjector` | Virtual gamepad input injection (test harnesses) |
| `IInteractiveContextRegistrar` | Interactive overlay/screen context declaration |
| `InteractiveContextHint` | Context declaration record (className + stick behaviors) |
| `StickBehavior` | Stick behavior modes for interactive contexts |
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
IInputInjector getInputInjector()                // null if unavailable (test harnesses)
IInteractiveContextRegistrar getInteractiveContextRegistrar()  // null if unavailable
```

### Utilities

```java
String getApiVersion()    // e.g. "0.8.6"
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

## IInputInjector

Virtual gamepad input injection for test harnesses / bridge mods. Injected state is **sticky** (a button stays pressed until released or cleared) and flows through the normal binding pipeline like real controller input.

```java
void pressButton(String buttonName, boolean pressed)  // ButtonName constants, e.g. "buttonA", "dpadUp"
void setAxis(String axisName, float value)            // -1.0..1.0; "leftStickX", "rightTrigger", ... 0 removes override
void clearAll()                                       // release all buttons, zero all axes
```

**Thread safety**: implementations must be safe to call from any thread; state is applied atomically on the client tick.

---

## IInteractiveContextRegistrar

Declare interactive overlays/screens. ControlFlex uses this to switch stick behavior (e.g. disable cursor control) while an interactive UI is open; it also auto-clears on phase exit as a fallback.

```java
void notifyOpen(InteractiveContextHint hint)   // entering an interactive overlay/screen
void notifyClose(String className)             // leaving it (paired with notifyOpen)
void clearAll()                                // fallback for mod unload / world exit
```

**Client thread only**; null-check `ControlFlexApi.getInteractiveContextRegistrar()` when ControlFlex may be absent.

---

## InteractiveContextHint / StickBehavior

`InteractiveContextHint` is the declaration record passed to `notifyOpen(...)`; a `null` side means "do not override that side".

```java
record InteractiveContextHint(String className, StickBehavior leftStick, StickBehavior rightStick)

StickBehavior.DEFAULT        // no override
StickBehavior.RADIAL_PATH    // stick positions cursor radially (ring selection)
StickBehavior.VIRTUAL_MOUSE  // stick drives the virtual mouse cursor
StickBehavior.DISABLED       // stick does not drive the cursor

// Construct via the static factory:
InteractiveContextHint.of("com.example.MyScreen", StickBehavior.DISABLED, null)
```

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
| `IInputInjector` | **Any thread** (applied atomically on client tick) |
| `IInteractiveContextRegistrar` | Client main thread |
