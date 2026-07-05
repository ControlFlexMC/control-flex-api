# control-flex-api

Public API for building ControlFlex bridge mods — read controller input, query action states, push mod states, and export compat configurations.

面向 ControlFlex 桥接模组开发者的公开 API — 读取手柄输入、查询动作状态、推送模组状态、导出兼容配置。

---

## 中文

### 这是什么？

`control-flex-api` 是 [ControlFlex](https://github.com/ControlFlexMC) 的公开 Java API。桥接模组（Bridge Mod）通过此 API 与 ControlFlex 通信，让其他 Minecraft 模组也能通过手柄操作。

### 依赖配置

```groovy
repositories {
    mavenLocal() // 或你使用的 Maven 仓库
}

dependencies {
    compileOnly 'com.ifels.controlflex:controlflex-api:0.8.5'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

### 5 分钟快速开始

```java
import com.ifels.controlflex.api.*;

// 1. 检查 ControlFlex 是否可用
if (!ControlFlexApi.isAvailable()) return;

// 2. 读取手柄输入
IInputProvider input = ControlFlexApi.getInputProvider();
if (input.isConnected()) {
    IControllerState state = input.getControllerState();
    float moveX = state.getLeftStickX();
    boolean jump = state.isButtonPressed(ButtonName.A);
}

// 3. 查询动作状态
IActionStateProvider actions = ControlFlexApi.getActionStateProvider();
if (actions.isGameActionActive("epicfight:key.epicfight.attack")) {
    // 处理攻击
}

// 4. 推送模组状态
IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
states.setState("mymod:combat_mode", true);
```

### 文档

| 文档 | 内容 |
|------|------|
| [快速开始](docs/getting-started.md) | 依赖配置、第一个插件、API 概述 |
| [API 参考](docs/api-reference.md) | 所有接口的完整签名和说明 |
| [Plugin 开发指南](docs/plugin-guide.md) | IControlFlexPlugin 生命周期、资源安装 |
| [Compat JSON 配置](docs/compat-config.md) | 无需代码即可适配模组按键 |
| [完整示例](docs/examples.md) | EpicFight 桥接等端到端示例 |

---

## English

### What is this?

`control-flex-api` is the public Java API for [ControlFlex](https://github.com/ControlFlexMC). Bridge mods use this API to communicate with ControlFlex, enabling controller support for other Minecraft mods.

### Dependency

```groovy
repositories {
    mavenLocal() // or your preferred Maven repository
}

dependencies {
    compileOnly 'com.ifels.controlflex:controlflex-api:0.8.5'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

### 5-Minute Quick Start

```java
import com.ifels.controlflex.api.*;

// 1. Check if ControlFlex is available
if (!ControlFlexApi.isAvailable()) return;

// 2. Read controller input
IInputProvider input = ControlFlexApi.getInputProvider();
if (input.isConnected()) {
    IControllerState state = input.getControllerState();
    float moveX = state.getLeftStickX();
    boolean jump = state.isButtonPressed(ButtonName.A);
}

// 3. Query action states
IActionStateProvider actions = ControlFlexApi.getActionStateProvider();
if (actions.isGameActionActive("epicfight:key.epicfight.attack")) {
    // Handle attack
}

// 4. Push mod state
IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
states.setState("mymod:combat_mode", true);
```

### Documentation

| Document | Content |
|----------|---------|
| [Getting Started](docs/getting-started.md) | Dependency setup, first plugin, API overview |
| [API Reference](docs/api-reference.md) | Complete interface signatures and descriptions |
| [Plugin Guide](docs/plugin-guide.md) | IControlFlexPlugin lifecycle, asset installation |
| [Compat JSON Config](docs/compat-config.md) | Adapt mod keys without writing code |
| [Examples](docs/examples.md) | EpicFight bridge and other end-to-end examples |

## License

MIT
