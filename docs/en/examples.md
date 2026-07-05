# Examples

## Example 1: Reading Controller Sticks

```java
import com.ifels.controlflex.api.*;

public class ControllerInputReader {

    private static final float DEADZONE = 0.15f;

    /** Returns {forward, strafe} in range -1.0~1.0 */
    public static float[] getMovement() {
        if (!ControlFlexApi.isAvailable()) return new float[]{0, 0};

        IInputProvider input = ControlFlexApi.getInputProvider();
        if (input == null || !input.isConnected()) return new float[]{0, 0};

        IControllerState state = input.getControllerState();
        if (state == null) return new float[]{0, 0};

        float forward = applyDeadzone(-state.getLeftStickY(), DEADZONE);
        float strafe  = applyDeadzone(state.getLeftStickX(), DEADZONE);

        return new float[]{forward, strafe};
    }

    private static float applyDeadzone(float v, float dz) {
        if (Math.abs(v) < dz) return 0;
        return Math.signum(v) * (Math.abs(v) - dz) / (1 - dz);
    }
}
```

## Example 2: Detecting Controller Actions

```java
public class ActionReader {

    public static boolean isAttacking() {
        IActionStateProvider actions = ControlFlexApi.getActionStateProvider();
        return actions != null && actions.isGameActionActive("attack");
    }

    public static boolean isModActionActive(String modId, String keyName) {
        IActionStateProvider actions = ControlFlexApi.getActionStateProvider();
        return actions != null
            && actions.isGameActionActive(modId + ":" + keyName);
    }
}
```

## Example 3: Pushing Mod State

```java
public class StatePusher {

    public static void onCombatModeChanged(boolean inCombat) {
        IPlayerStateRegistry registry = ControlFlexApi.getPlayerStateRegistry();
        if (registry != null) {
            registry.setState("mymod:combat_mode", inCombat);
        }
    }
}
```

With compat JSON:

```json
"itemSuppressKeys": [{
    "item": "mymod:special_weapon",
    "playerState": ["mymod:combat_mode"],
    "suppressActions": ["attack"]
}]
```

## Example 4: Complete Bridge Mod

Project structure example (`cfx-compat-epicfight`):

```
cfx-compat-epicfight/
├── build.gradle
└── src/main/
    ├── java/com/ifels/cfx/epicfight/
    │   ├── CfxEpicFightMod.java
    │   ├── CfxEpicFightPlugin.java        ← IControlFlexPlugin impl
    │   ├── CfxEpicFightControllerMod.java
    │   ├── EpicFightStateBridge.java
    │   └── ...
    └── resources/
        ├── META-INF/
        │   ├── mods.toml
        │   └── services/com.ifels.controlflex.api.IControlFlexPlugin
        └── assets/cfx_compat_epicfight/
            ├── compat/epicfight_keys.json
            ├── guides/epicfight_guid.json
            └── lang/{en_us,zh_cn}.json
```

### CfxEpicFightPlugin.java

```java
public class CfxEpicFightPlugin implements IControlFlexPlugin {

    @Override public String getModId() { return "cfx_compat_epicfight"; }

    @Override
    public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
        installer.install(
            "/assets/cfx_compat_epicfight/compat/epicfight_keys.json",
            "epicfight_keys.json");
    }

    @Override
    public void onInstallGuideAssets(ICompatAssetInstaller installer) {
        installer.install(
            "/assets/cfx_compat_epicfight/guides/epicfight_guid.json",
            "epicfight_guid.json");
    }

    @Override
    public void onControlFlexReady() {
        if (!requireApiVersion("0.8.5")) return;
        if (!ControlFlexApi.isAvailable()) return;
        stateBridge.initialize();
    }
}
```

### build.gradle

```groovy
plugins { id 'net.neoforged.moddev.legacyforge' }

base { archivesName = 'cfx-compat-epicfight' }

def modArtifactVersion = build_id ? "${mod_version}.${build_id}" : mod_version
version = "${modArtifactVersion}-mc${minecraft_version}-forge"

repositories {
    mavenLocal()
    maven { url 'https://cursemaven.com' }
}

dependencies {
    compileOnly 'com.ifels.controlflex:controlflex-api:0.8.5'
    compileOnly 'curse.maven:epicfight-405076:7789099'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

## Example 5: Adapting UI with IControllerCapabilities

```java
public class GlyphHelper {

    public static String getAttackGlyph() {
        IInputProvider input = ControlFlexApi.getInputProvider();
        if (input == null) return "[Attack]";

        IControllerCapabilities caps = input.getCapabilities();
        if (caps == null) return "[A]";

        return switch (caps.getControllerType()) {
            case PLAYSTATION -> "✕";   // ✕
            case XBOX         -> "A";
            case NINTENDO_SWITCH -> "B";
            default           -> "A";
        };
    }
}
```
