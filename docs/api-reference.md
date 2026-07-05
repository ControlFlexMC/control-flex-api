# API 参考

> 包路径: `com.ifels.controlflex.api`  
> 版本: 0.8.5  
> 线程模型: 所有方法必须在客户端主线程调用（除非特别标注）

## 类型清单

| 类型 | 说明 |
|------|------|
| `ControlFlexApi` | 静态入口，获取所有 provider |
| `ButtonName` | 按钮/轴名称常量 |
| `IControlFlexPlugin` | SPI 插件接口 |
| `ICompatAssetInstaller` | 资源安装器（SPI 回调参数） |
| `IActionStateProvider` | 动作状态查询 |
| `IInputProvider` | 控制器输入查询 |
| `IControllerState` | 控制器硬件状态（实时视图） |
| `IControllerCapabilities` | 控制器硬件能力 |
| `IPlayerStateRegistry` | 第三方模组状态推送 |
| `InputMode` | 输入模式（KEYBOARD_MOUSE / MIXED） |
| `ControllerType` | 控制器类型枚举 |

---

## ControlFlexApi

桥接模组唯一入口。所有方法在 ControlFlex 未安装/未初始化时返回安全默认值。

```java
public final class ControlFlexApi
```

### 可用性

```java
boolean isAvailable()               // ControlFlex 已安装并完全初始化
boolean isControllerConnected()     // 有控制器连接
```

### Provider 获取

```java
IActionStateProvider getActionStateProvider()    // null = 不可用
IInputProvider getInputProvider()                // null = 不可用
IPlayerStateRegistry getPlayerStateRegistry()    // null = 不可用
```

### 工具

```java
String getApiVersion()    // e.g. "0.8.5"
void reloadGuides()       // 重新加载 guide 配置
```

---

## ButtonName

按钮和轴的名称常量，配合 `isButtonPressed(String)` 和 `getAxisValue(String)` 使用。

```java
// 按钮
ButtonName.A, ButtonName.B, ButtonName.X, ButtonName.Y
ButtonName.DPAD_UP, ButtonName.DPAD_DOWN, ButtonName.DPAD_LEFT, ButtonName.DPAD_RIGHT
ButtonName.LEFT_BUMPER, ButtonName.RIGHT_BUMPER
ButtonName.LEFT_TRIGGER, ButtonName.RIGHT_TRIGGER
ButtonName.LEFT_STICK_CLICK, ButtonName.RIGHT_STICK_CLICK
ButtonName.BACK, ButtonName.START, ButtonName.GUIDE
ButtonName.PADDLE_1 ~ ButtonName.PADDLE_4
ButtonName.TOUCHPAD

// 轴 (for getAxisValue)
ButtonName.AXIS_LEFT_X, ButtonName.AXIS_LEFT_Y
ButtonName.AXIS_RIGHT_X, ButtonName.AXIS_RIGHT_Y
ButtonName.AXIS_LEFT_TRIGGER, ButtonName.AXIS_RIGHT_TRIGGER
```

---

## IControlFlexPlugin

桥接模组的 SPI 接口。通过 `META-INF/services/` 注册。

```java
String getModId()

// 生命周期回调（按调用顺序）:
void onInstallCompatConfigs(ICompatAssetInstaller installer)   // 导出 compat JSON
void onInstallGuideAssets(ICompatAssetInstaller installer)     // 导出 guide JSON
void onControlFlexReady()                                       // ControlFlex 就绪
void onControllerConnectionChanged(boolean connected)           // 控制器插拔

// 工具:
boolean requireApiVersion(String minVersion)  // 版本检查，不满足时返回 false
```

### 生命周期时序

```
ControlFlex 启动
  └── initializeClient()
        → API providers 注入完成 (isAvailable() = true)
        → 提取内置配置到 default/ 目录

  └── onKeyMappingsReady()
        └── 发现 SPI 插件
              ├── new CompatAssetInstaller(plugin.class, compatDir)
              ├── plugin.onInstallCompatConfigs(installer)
              ├── new CompatAssetInstaller(plugin.class, guideDir)
              ├── plugin.onInstallGuideAssets(installer)
              └── plugin.onControlFlexReady()

  运行时:
        └── plugin.onControllerConnectionChanged(connected)
```

---

## ICompatAssetInstaller

资源安装器。ControlFlex 创建实例并注入插件的 ClassLoader + 目标目录。

```java
boolean install(String resourcePath, String fileName)
// resourcePath: classpath path e.g. "/assets/mymod/compat/epicfight_keys.json"
// fileName:      target filename in cfx-mod/ directory
// returns:       true if installed successfully
```

### 使用示例

```java
@Override
public void onInstallCompatConfigs(ICompatAssetInstaller installer) {
    installer.install("/assets/mymod/compat/epicfight_keys.json", "epicfight_keys.json");
}

@Override
public void onInstallGuideAssets(ICompatAssetInstaller installer) {
    installer.install("/assets/mymod/guides/epicfight_guid.json", "epicfight_guid.json");
}
```

---

## IActionStateProvider

查询 ControlFlex 当前激活的动作。

```java
boolean isGameActionActive(String actionId)   // 游戏层动作是否激活
boolean isGuiActionActive(String actionId)    // GUI 层动作是否激活
Set<String> getActiveGameActions()             // 所有激活的游戏层动作（实时视图，勿缓存）
Set<String> getActiveGuiActions()              // 所有激活的 GUI 层动作（实时视图，勿缓存）
```

**动作 ID 格式**:
- 内置动作: `"attack"`, `"use"`, `"jump"`
- 原版按键: `"key.jump"`, `"key.sneak"`
- 模组按键: `"epicfight:key.epicfight.attack"` (modId:keyName)

---

## IInputProvider

控制器硬件状态查询。

```java
boolean isConnected()
IControllerState getControllerState()          // null = 无连接（实时视图，勿缓存）
String getGamepadName()                         // null = 无连接
int getGamepadIndex()                           // -1 = 无连接
IControllerCapabilities getCapabilities()       // null = 无连接
```

---

## IControllerState

控制器硬件状态实时视图。摇杆 -1.0~1.0，扳机 0.0~1.0。Y 轴正值向下。

```java
// 摇杆
float getLeftStickX() / getLeftStickY()
float getRightStickX() / getRightStickY()

// 扳机
float getLeftTrigger() / getRightTrigger()
boolean isLeftTriggerPressed() / isRightTriggerPressed()   // 阈值 0.5

// 按钮 (camelCase)
boolean isButtonAPressed() / isButtonBPressed() / isButtonXPressed() / isButtonYPressed()
boolean isButtonBackPressed() / isButtonStartPressed() / isButtonGuidePressed()
boolean isLeftBumperPressed() / isRightBumperPressed()
boolean isLeftStickClicked() / isRightStickClicked()
boolean isDpadUpPressed() / isDpadDownPressed() / isDpadLeftPressed() / isDpadRightPressed()
boolean isPaddle1Pressed() ~ isPaddle4Pressed()
boolean isTouchpadPressed()
boolean isShiftLayerActive()

// 字符串查询
boolean isButtonPressed(String buttonName)   // 推荐用 ButtonName 常量
float getAxisValue(String axisName)          // 推荐用 ButtonName.AXIS_* 常量
```

---

## IControllerCapabilities

控制器硬件能力查询。

```java
ControllerType getControllerType()   // XBOX / PLAYSTATION / NINTENDO_SWITCH / ...
boolean hasPaddles()                  // 有背键
int getPaddleCount()                  // 背键数量
boolean hasTouchpad()                 // 有触摸板
boolean hasGyro()                     // 有陀螺仪
boolean hasRumble()                   // 有震动
boolean hasAnalogTriggers()           // 模拟扳机 (vs 数字)
boolean isNintendoLayout()            // 任天堂布局 (A/B 互换)
```

---

## IPlayerStateRegistry

桥接模组向 ControlFlex 推送状态，供 compat JSON 中的 `playerState` 条件使用。

```java
void setState(String stateKey, boolean active)  // 线程安全
boolean getState(String stateKey)               // 客户端线程
void clearState(String stateKey)                // 删除注册
```

**stateKey 格式**: `"modId:stateName"` (如 `"epicfight:battle_mode"`)

---

## 线程模型

| 接口 | 约束 |
|------|------|
| `ControlFlexApi` | 客户端主线程 |
| `IActionStateProvider` | 客户端主线程 |
| `IInputProvider` / `IControllerState` | 客户端主线程（底层 volatile 保证可见性） |
| `IControllerCapabilities` | 客户端主线程 |
| `IPlayerStateRegistry.setState()` | **任意线程** |
| `IPlayerStateRegistry.getState()` | 客户端主线程 |
