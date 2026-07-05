# 快速开始

## 1. 项目配置

### build.gradle

```groovy
plugins {
    id 'net.neoforged.moddev.legacyforge'  // Forge
    // 或 'fabric-loom'                     // Fabric
}

repositories {
    mavenLocal()
    // 后续可通过 CurseMaven / Modrinth Maven 获取
}

dependencies {
    // ControlFlex API — compileOnly，运行时由 ControlFlex 主模组提供
    compileOnly 'com.ifels.controlflex:controlflex-api:0.8.5'
    compileOnly 'org.jetbrains:annotations:24.0.1'
}
```

### mods.toml (Forge)

```toml
[[dependencies.your_mod_id]]
    modId = "controlflex"
    mandatory = true
    versionRange = "[0.8.5,)"
    ordering = "AFTER"
    side = "CLIENT"
```

### fabric.mod.json (Fabric)

```json
{
  "depends": {
    "controlflex": ">=0.8.5"
  }
}
```

## 2. API 可用性

**始终先检查 API 是否可用**。ControlFlex 可能未安装或 SDL3 初始化失败。

```java
if (!ControlFlexApi.isAvailable()) {
    // 回退：不做任何手柄相关处理
    return;
}
```

## 3. API 总览

```
ControlFlexApi (静态入口)
├── getActionStateProvider()  → IActionStateProvider  查询动作状态
├── getInputProvider()        → IInputProvider        读取手柄输入
│                              ├── getControllerState() → IControllerState
│                              └── getCapabilities()    → IControllerCapabilities
├── getPlayerStateRegistry()  → IPlayerStateRegistry  推送模组状态
└── isAvailable() / isControllerConnected() / getApiVersion()
```

## 4. 两种适配方式

| | Compat JSON | Bridge Mod (Java) |
|---|---|---|
| **怎么做** | 写 JSON 配置文件 | 实现 IControlFlexPlugin (SPI) |
| **能力** | 按键分类、事件控制、条件抑制 | 完全自定义：输入读取、状态同步 |
| **适用** | 模组按键已被 MC KeyMapping 注册 | 模组有独立输入系统 |
| **示例** | JEI, MineMenu | EpicFight (战斗模式状态) |

## 5. 第一个 Bridge Mod

创建 SPI 插件实现：

```java
package com.example.mymod;

import com.ifels.controlflex.api.*;

public class MyPlugin implements IControlFlexPlugin {

    @Override
    public String getModId() {
        return "my_bridge_mod";
    }

    @Override
    public void onControlFlexReady() {
        if (!requireApiVersion("0.8.5")) return;
        if (!ControlFlexApi.isAvailable()) return;

        // 初始化你的桥接逻辑
        IPlayerStateRegistry states = ControlFlexApi.getPlayerStateRegistry();
        states.setState("mymod:initialized", true);
    }
}
```

SPI 注册文件 `src/main/resources/META-INF/services/com.ifels.controlflex.api.IControlFlexPlugin`：

```
com.example.mymod.MyPlugin
```

这就是全部——无需手动注册，ControlFlex 自动发现。

## 6. 下一步

- [Plugin 开发指南](plugin-guide.md) — 生命周期详解、资源安装
- [API 参考](api-reference.md) — 完整接口文档
- [Compat JSON 配置](compat-config.md) — 不写代码适配模组
- [完整示例](examples.md) — EpicFight 桥接模组
