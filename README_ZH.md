# control-flex-api

面向 ControlFlex 桥接模组开发者的公开 API — 读取手柄输入、查询动作状态、推送模组状态、导出兼容配置。

[English](README.md)

## 文档

| 文档 | 内容 |
|------|------|
| [快速开始](docs/zh/getting-started.md) | 项目配置、API 总览、第一个插件 |
| [API 参考](docs/zh/api-reference.md) | 所有接口的完整签名和说明 |
| [Plugin 开发指南](docs/zh/plugin-guide.md) | 生命周期、资源安装、完整模板 |
| [Compat JSON 配置](docs/zh/compat-config.md) | 无需代码即可适配模组按键 |
| [完整示例](docs/zh/examples.md) | EpicFight 桥接等端到端代码 |

## 快速开始

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

// 始终先检查可用性
if (!ControlFlexApi.isAvailable()) return;

// 读取手柄输入
IInputProvider input = ControlFlexApi.getInputProvider();
if (input.isConnected()) {
    IControllerState state = input.getControllerState();
    float moveX = state.getLeftStickX();
    boolean jump = state.isButtonPressed(ButtonName.A);
}

// 推送模组状态
IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
states.setState("mymod:combat_mode", true);
```

## 许可

MIT
