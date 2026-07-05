# 完整示例

## 示例 1: 读取手柄摇杆

```java
import com.ifels.controlflex.api.*;

public class ControllerInputReader {

    private static final float DEADZONE = 0.15f;

    /** 返回 {forward, strafe}，范围 -1.0~1.0 */
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

## 示例 2: 检测手柄动作

```java
public class ActionReader {

    public static boolean isAttacking() {
        IActionStateProvider actions = ControlFlexApi.getActionStateProvider();
        return actions != null && actions.isGameActionActive("attack");
    }

    public static boolean isModActionActive(String modId, String keyName) {
        IActionStateProvider actions = ControlFlexApi.getActionStateProvider();
        if (actions == null) return false;
        return actions.isGameActionActive(modId + ":" + keyName);
    }

    // 用法: ActionReader.isModActionActive("epicfight", "key.epicfight.dodge")
}
```

## 示例 3: 推送模组状态

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

配合 compat JSON:

```json
"itemSuppressKeys": [{
    "item": "mymod:special_weapon",
    "playerState": ["mymod:combat_mode"],
    "suppressActions": ["attack"]
}]
```

## 示例 4: 完整 Bridge Mod

以 `cfx-compat-epicfight` 为例:

```
cfx-compat-epicfight/
├── build.gradle
└── src/main/
    ├── java/com/ifels/cfx/epicfight/
    │   ├── CfxEpicFightMod.java           # @Mod 主类
    │   ├── CfxEpicFightPlugin.java        # IControlFlexPlugin 实现
    │   ├── CfxEpicFightControllerMod.java # EpicFight IEpicFightControllerMod
    │   ├── EpicFightStateBridge.java      # 状态检测 + 推送
    │   ├── CfxControllerBinding.java       # 手柄绑定映射
    │   ├── CfxMovementBinding.java         # 摇杆移动映射
    │   ├── ActionBindingMapper.java        # 动作 ID 映射
    │   └── mixin/                          # Mixin 注入
    └── resources/
        ├── META-INF/
        │   ├── mods.toml
        │   └── services/com.ifels.controlflex.api.IControlFlexPlugin
        ├── assets/cfx_compat_epicfight/
        │   ├── compat/epicfight_keys.json
        │   ├── guides/epicfight_guid.json
        │   └── lang/{en_us,zh_cn}.json
        └── cfx-compat-epicfight.mixins.json
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

    @Override
    public void onControllerConnectionChanged(boolean connected) {
        // 处理控制器插拔
    }
}
```

### build.gradle 关键配置

```groovy
plugins {
    id 'net.neoforged.moddev.legacyforge'
}

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

## 示例 5: 使用 IControllerCapabilities 适配 UI

```java
public class GlyphHelper {

    public static String getAttackGlyph() {
        IInputProvider input = ControlFlexApi.getInputProvider();
        if (input == null) return "[Attack]";

        IControllerCapabilities caps = input.getCapabilities();
        if (caps == null) return "[A]";

        return switch (caps.getControllerType()) {
            case PLAYSTATION -> "[✕]";
            case XBOX         -> "[A]";
            case NINTENDO_SWITCH -> "[B]";
            default           -> "[A]";
        };
    }
}
```
