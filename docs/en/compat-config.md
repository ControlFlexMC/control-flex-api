# Compat JSON Configuration

Adapt third-party mod keys for controller support without writing Java code.

## File Location

```
config/controlflex/compat/default/{modid}_keys.json   ← ControlFlex built-in
config/controlflex/compat/cfx-mod/{modid}_keys.json    ← Bridge mod (onInstallCompatConfigs)
config/controlflex/compat/user/{modid}_keys.json       ← User customizations
```

Priority: `user > cfx-mod > default`. Same mod_id = complete override.

## JSON Format

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

## Top-level Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `mod_id` | String | Yes | Target mod ID |
| `loader` | String[] | No | Loader filter: `["forge"]`, `["fabric"]`, `["forge","fabric"]`; omit = all |
| `versions` | Array | Yes | Version-specific configs, first match wins |

## Version Matching

| Format | Example | Meaning |
|--------|---------|---------|
| Wildcard | `""` | All versions |
| Exact | `"1.21.1"` | Exact match |
| Interval | `"[1.0,2.0)"` | `1.0 ≤ v < 2.0` |
| Unbounded | `"[21.1,)"` | `v ≥ 21.1` |
| Unbounded | `"(,1.20]"` | `v ≤ 1.20` |

> Tip: Put specific version entries first, wildcard `""` last as fallback.

## versions[] Fields

### guiKeys

Keys active only when a Screen is open.

```json
"guiKeys": ["key.jei.showRecipe", "key.mymod.config_gui"]
```

### ignoreKeys

Keys hidden from the binding UI (debug keys, etc.).

```json
"ignoreKeys": ["key.mymod.debug_info"]
```

### skipForgeKeys & skipVanillaKeys

| Config | KeyMapping.setPressed() | Forge InputEvent | Use case |
|--------|------------------------|------------------|----------|
| Default | ✅ | ✅ | Most mods |
| `skipForgeKeys` | ✅ | ❌ | Double-trigger |
| `skipVanillaKeys` | ❌ | ✅ | Forge-only listener |

```json
"skipForgeKeys": ["key.epicfight.attack"],
"skipVanillaKeys": []
```

### specialActionKeys

```json
"specialActionKeys": [
  { "keyName": "key.mymod.radial_menu", "flags": ["PHASE_PERSISTENT"] },
  { "keyName": "key.mymod.open_menu", "flags": ["GLFW_COMPAT"] }
]
```

| Flag | Meaning |
|------|---------|
| `GLFW_COMPAT` | Synthetic InputConstants.isKeyDown() polling state |
| `PHASE_PERSISTENT` | Keep key pressed across Screen transitions |

### itemSuppressKeys

Suppress actions when holding specific items.

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

| Field | Type | Description |
|-------|------|-------------|
| `item` | String | Item ID, supports `*` wildcard; mutually exclusive with `tag` |
| `tag` | String | Item tag; mutually exclusive with `item` |
| `playerState` | String[] | State conditions (OR); omit = unconditional |
| `suppressActions` | String[] | Action IDs to suppress |

### tips

Hint text in the binding UI.

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

Per-Screen stick behavior.

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

Modes: `DEFAULT` / `RADIAL_CURSOR` / `VIRTUAL_MOUSE` / `DISABLED`

## Debugging

Check the logs after launch:

```
[ModCompat] Loaded default/epicfight_keys.json for mod epicfight
[ModCompat] cfx-mod/epicfight_keys.json overrides epicfight from lower layer
```
