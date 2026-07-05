# Compat JSON 配置指南

无需编写 Java 代码即可让第三方模组的按键支持手柄操作。

## 文件位置

```
config/controlflex/compat/default/{modid}_keys.json   ← ControlFlex 内置
config/controlflex/compat/mods/{modid}_keys.json    ← 桥接模组 (onInstallCompatConfigs)
config/controlflex/compat/user/{modid}_keys.json       ← 用户自定义
```

优先级: `user > mods > default`，同 mod_id 完全覆盖。

## JSON 格式

```json
{
  "mod_id": "targetmod",
  "loader": ["forge"],
  "versions": [
    {
      "version": "[2.0,)",
      "guiKeys": ["key.targetmod.config_gui"],
      "ignoreKeys": ["key.targetmod.debug"],
      "skipForgeKeys": ["key.targetmod.dash"],
      "skipVanillaKeys": [],
      "specialActionKeys": [],
      "itemSuppressKeys": [],
      "tips": [],
      "leftStick": [],
      "rightStick": []
    },
    {
      "version": "",
      "guiKeys": ["key.targetmod.basic_gui"]
    }
  ]
}
```

## 顶层字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `mod_id` | String | 是 | 目标模组的 Mod ID |
| `loader` | String[] | 否 | 加载器限制: `["forge"]`, `["fabric"]`, `["forge","fabric"]`；省略 = 全平台 |
| `versions` | Array | 是 | 版本配置列表，按数组顺序匹配 |

## 版本匹配

`version` 字段支持三种形式，**首个匹配即生效**:

| 格式 | 示例 | 语义 |
|------|------|------|
| 通配符 | `""` | 匹配所有版本 |
| 精确 | `"1.21.1"` | 仅匹配此版本 |
| 区间 | `"[1.0,2.0)"` | `1.0 ≤ v < 2.0` |
| 区间 | `"[21.1,)"` | `v ≥ 21.1` |
| 区间 | `"(,1.20]"` | `v ≤ 1.20` |

> 推荐: 具体版本条目在前，通配符 `""` 在最后作 fallback。

## versions[] 字段

### guiKeys

GUI 层按键。只在 Screen 打开时激活。

```json
"guiKeys": ["key.jei.showRecipe", "key.mymod.config_gui"]
```

### ignoreKeys

从绑定界面隐藏的按键（调试键、配置键等）。

```json
"ignoreKeys": ["key.mymod.debug_info"]
```

### skipForgeKeys & skipVanillaKeys

ControlFlex 默认同时发送两条事件路径:

| 配置 | KeyMapping.setPressed() | Forge InputEvent | 场景 |
|------|------------------------|------------------|------|
| 默认 | ✅ | ✅ | 大多数模组 |
| `skipForgeKeys` | ✅ | ❌ | 双重触发 |
| `skipVanillaKeys` | ❌ | ✅ | 仅 Forge 事件 |

```json
"skipForgeKeys": ["key.epicfight.attack"],
"skipVanillaKeys": []
```

### specialActionKeys

特殊标记按键。

```json
"specialActionKeys": [
  {
    "keyName": "key.mymod.radial_menu",
    "flags": ["PHASE_PERSISTENT"]
  },
  {
    "keyName": "key.mymod.open_menu",
    "flags": ["GLFW_COMPAT"]
  }
]
```

| Flag | 含义 |
|------|------|
| `GLFW_COMPAT` | 合成 InputConstants.isKeyDown() 轮询状态 |
| `PHASE_PERSISTENT` | Screen 切换时保持按键状态 |

### itemSuppressKeys

持有特定物品时抑制按键。

```json
"itemSuppressKeys": [
  {
    "item": "mymod:legendary_sword",
    "suppressActions": ["attack", "use"]
  },
  {
    "tag": "forge:swords",
    "playerState": ["mymod:berserk_mode"],
    "suppressActions": ["attack"]
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `item` | String | 物品 ID，支持 `*` 通配符；与 `tag` 互斥 |
| `tag` | String | 物品标签；与 `item` 互斥 |
| `playerState` | String[] | 状态条件 (OR)；省略 = 无条件 |
| `suppressActions` | String[] | 抑制的动作 ID（短名如 `"attack"`，全 ID 如 `"mymod:key.mymod.dash"`） |

### tips

绑定界面的提示文字。

```json
"tips": [
  {
    "key": "key.mymod.ultimate",
    "text": {
      "en_us": "Bind to RT, use HOLD mode",
      "zh_cn": "绑定到 RT，使用 HOLD 模式"
    }
  }
]
```

### leftStick & rightStick

特定 Screen 的摇杆行为。

```json
"leftStick": [
  {
    "screens": ["com.mymod.RadialScreen"],
    "mode": "RADIAL_CURSOR",
    "threshold": 0.5,
    "cursorRadius": 100
  }
]
```

mode: `DEFAULT` / `RADIAL_CURSOR` / `VIRTUAL_MOUSE` / `DISABLED`

## 调试

启动后查看日志:

```
[ModCompat] Loaded default/epicfight_keys.json for mod epicfight
[ModCompat] mods/epicfight_keys.json overrides epicfight from lower layer
```
