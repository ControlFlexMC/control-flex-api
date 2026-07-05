# control-flex-api

Public API for building ControlFlex bridge mods — read controller input, query action states, push mod states, and export compat configurations.

面向 ControlFlex 桥接模组开发者的公开 API — 读取手柄输入、查询动作状态、推送模组状态、导出兼容配置。

---

## [中文文档](docs/zh/)

| 文档 | 内容 |
|------|------|
| [快速开始](docs/zh/getting-started.md) | 项目配置、API 总览、第一个插件 |
| [API 参考](docs/zh/api-reference.md) | 所有接口的完整签名和说明 |
| [Plugin 开发指南](docs/zh/plugin-guide.md) | 生命周期、资源安装、完整模板 |
| [Compat JSON 配置](docs/zh/compat-config.md) | 无需代码即可适配模组按键 |
| [完整示例](docs/zh/examples.md) | EpicFight 桥接等端到端代码 |

### 快速开始

```groovy
// build.gradle
dependencies {
    compileOnly 'com.ifels.controlflex:controlflex-api:0.8.5'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

```java
if (!ControlFlexApi.isAvailable()) return;

IInputProvider input = ControlFlexApi.getInputProvider();
if (input.isConnected()) {
    IControllerState state = input.getControllerState();
    float moveX = state.getLeftStickX();
}
```

---

## [English Docs](docs/en/)

| Document | Content |
|----------|---------|
| [Getting Started](docs/en/getting-started.md) | Project setup, API overview, first plugin |
| [API Reference](docs/en/api-reference.md) | Complete interface signatures and descriptions |
| [Plugin Guide](docs/en/plugin-guide.md) | Lifecycle, asset installation, full template |
| [Compat JSON Config](docs/en/compat-config.md) | Adapt mod keys without writing code |
| [Examples](docs/en/examples.md) | EpicFight bridge and other end-to-end code |

### Quick Start

```groovy
// build.gradle
dependencies {
    compileOnly 'com.ifels.controlflex:controlflex-api:0.8.5'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

```java
if (!ControlFlexApi.isAvailable()) return;

IInputProvider input = ControlFlexApi.getInputProvider();
if (input.isConnected()) {
    IControllerState state = input.getControllerState();
    float moveX = state.getLeftStickX();
}
```

## License

MIT
