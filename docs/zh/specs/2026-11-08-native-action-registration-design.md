# 原生动作注册：桥接模组直接声明动作与默认绑定（0.14.0）

> 英文版：[English](../../en/specs/2026-11-08-native-action-registration-design.md)
**状态：** 待评审（形状已定，实现未开始）
**目标版本：** `0.14.0`
**日期：** 2026-11-08
**前置：** 0.13.0 未打 tag、未发布，`IScreenHandler` / `IActionBindingProvider` / 内置 `hud_toggle` 都还在本地。
**参考：** `cfx-compat-vehicles/docs/superpowers/specs/2026-11-08-native-action-registration-design.md`（游戏层 native 动作的先行方案）。

## 1. 问题

### 1.1 现状

`cfx-compat-cobblemon` 的 18 个战斗动作走的是老路：

1. 桥接注册 18 个伪 `KeyMapping`（`cfx_compat_cobblemon` 命名空间）；
2. 由一份 compat JSON 的 `screenKeys` 段把它们归属到 `cobblemon`；
3. ControlFlex 把 KeyMapping 重新归属成 `cobblemon:key.cfx_compat_cobblemon.battle_*`，桥接再用 `candidateIds()` 容忍 id 改名；
4. GUI 候选表按 `guiActionBindings` 的键遍历，每屏覆盖只能改“已有键的按钮”。

### 1.2 这条路的三个代价

- **向 MC 全局暴露**：18 个伪 KeyMapping 进入原版控制页和全局键状态，但它们只在 `BattleGUI` 上有意义。
- **候选资格依赖一张全局表**：`hud_toggle` 曾经“设置界面能绑、运行时永不派发”，根因就是 GUI 候选表里没有这个键。任何新动作都要记得往全局表里补一行。
- **默认值没有落点**：GUI 层的默认绑定是全局的，无法表达“这个默认只在 `BattleGUI` 上成立”。

### 1.3 机会

这些动作、API、核心实现都没有对外发布，可以整体改成“桥接直接向 ControlFlex 声明动作”，不再经过 MC 的 KeyMapping 与全局 GUI 层。

## 2. 目标与非目标

目标：

- 桥接模组可注册“只有 ControlFlex 可见”的动作：稳定 id、显示名、归属 mod、归属 layer、可选的 Screen/Overlay 类过滤、可选默认绑定。
- 注册的动作不进入 MC 的 `KeyMapping` 注册表，不写全局键状态。
- Screen/Overlay 动作**不进入** `guiActionBindings` / `overlayActionBindings` 的全局映射表。
- 动作触发时由 ControlFlex 回调桥接，桥接不需要轮询按键状态。
- 默认绑定在用户没有显式配置时生效；用户清空后不被重新种入。
- 游戏层动作保留一个电平 handle（`isActive()`），供载具这类“按住”语义使用。

非目标：

- 不做模拟量（`getValue()` 只预留）。
- 不做运行时注销 / 热重载。
- 不改 `IScreenHandler`（返回键 + HUD 状态）与 `IActionBindingProvider`（提示查询）的既有契约。
- 不为内置 `hud_toggle` 增加专属动作或默认绑定。
- 不把内置 / 旧动态动作迁到新存储（它们继续走原路径，见 §6.4）。

## 3. 概念

### 3.1 注册项

一次注册声明五类信息 + 一条默认绑定：

| 字段 | 例子 | 规则 |
|---|---|---|
| `actionId` | `cfx_compat_cobblemon:battle_fight` | 必须含 `:`，前缀必须是插件 `getModId()` |
| `nameKey` | `key.cfx_compat_cobblemon.battle_fight` | i18n key；为空时回退 actionId |
| `groupModId` | `cobblemon` | UI 分组；未加载时回退插件 mod id |
| `layers` | `{SCREEN}` | 可多选；一个都不声明 = `IN_GAME + SCREEN + OVERLAY` 全归属 |
| `classes` | `{...BattleGUI}` | 只对 SCREEN / OVERLAY 有意义；不声明 = 该层全部；精确匹配，不继承 |
| `default` | `leftBumper|PRESS` | 一条；种到该动作声明的每个 layer / 每个 class；不声明 = 全部留空 |

### 3.2 三种归属

| layer | 对应内部相位 | 绑定存储 | 默认落点 | 派发时机 |
|---|---|---|---|---|
| `IN_GAME` | GAME（main / shift1..4） | profile 的层映射 | 缺键时种入各游戏层 | 无 Screen / Overlay 前台时 |
| `SCREEN` | GUI | 每屏绑定表（`user_screen_overrides.json`） | 该 Screen 类下的解析兜底 | 该 Screen 类在前台 |
| `OVERLAY` | OVERLAY | 每屏绑定表（`user_screen_overrides.json`） | 该 Overlay 类下的解析兜底 | 该 Overlay 类在前台 |

Screen/Overlay 动作**不写入** `guiActionBindings` / `overlayActionBindings`；候选资格由“注册作用域 + 绑定是否存在”决定，不再查全局表。

### 3.3 触发语义

回调描述的是**状态变化的边沿**，不是每 tick 的重复：

| 模式 | `onActionTriggered` | `onActionReleased` | `isActive()` |
|---|---|---|---|
| `PRESS` / `DELAY_PRESS` / `LONG_PRESS` | 触发那一次 | 不回调 | 仅触发当 tick 为 true |
| `RELEASE` / `TAP` / `DOUBLE_TAP` | 抬起判定成立那一次 | 不回调 | 仅触发当 tick 为 true |
| `DOUBLE_PRESS` | 第二次按下判定成立那一次 | 不回调 | 仅触发当 tick 为 true |
| `HOLD` / `DELAY_HOLD` / `LONG_HOLD` / `DOUBLE_HOLD` | 进入激活状态的那一次 | 离开激活状态时一次 | 按住期间 true |
| `TOGGLE` / `DOUBLE_TOGGLE` | 每次切到 on | 每次切到 off | 锁存期间 true |

- 即使 HOLD 的绑定在 GUI 相位每 tick 都参与组合派发，回调也只在激活边沿发一次（`hud_toggle` 曾被绑成 HOLD 后每秒翻 20 次，就是漏了这条约束）。
- 一次性模式的 `isActive()` 由一个“最后触发 tick”字段实现：触发当 tick 为 true，下一 tick 自动回落 false，不依赖 GUI 活动集（后者只跟踪 HOLD）。
- 回调在客户端主线程、输入 tick 内执行，早于该帧渲染。异常被隔离并记录，不中断其他动作的派发。

## 4. API

```java
public enum ActionLayer { IN_GAME, SCREEN, OVERLAY }

public interface IActionRegistrar {
    /** 重复 id 幂等返回已有 handle；非法参数记警告并返回永不触发的 handle。 */
    IControlFlexAction register(ActionRegistration registration);
}

public final class ActionRegistration {
    public static Builder builder(String actionId);

    public static final class Builder {
        public Builder nameKey(String nameKey);
        public Builder groupModId(String modId);
        public Builder layer(ActionLayer layer);          // 可多次调用
        public Builder targetClass(String className);     // 仅 SCREEN / OVERLAY
        public Builder defaultBinding(DefaultBinding b);  // 可选
        public ActionRegistration build();
    }
}

public final class DefaultBinding {
    /** button 使用 ButtonName 常量；不把 "leftStickUp|HOLD" 暴露给调用方。 */
    public static DefaultBinding of(String button, TriggerMode mode);
}

public interface IControlFlexAction {
    String getActionId();
    /** 电平：见 §3.3 的 isActive 列。未绑定 / 已被清空时恒为 false。主线程，每 tick 可读。 */
    boolean isActive();
}

public interface ActionTrigger {
    String actionId();
    TriggerMode mode();
    ActionLayer layer();
    /** 命中的 Screen / Overlay 类名；IN_GAME 时为 null。 */
    String targetClassName();
}

public interface IControlFlexPlugin {
    /** 在 onControlFlexReady 之前、profile 载入之前调用；registrar 只在回调期间有效。 */
    default void onRegisterActions(IActionRegistrar registrar) {}

    default void onActionTriggered(ActionTrigger trigger) {}

    /** 进入非激活状态（HOLD 系 / TOGGLE 系）；一次性模式不回调。 */
    default void onActionReleased(String actionId) {}
}
```

校验：

| 条件 | 行为 |
|---|---|
| id 为空 / 不含 `:` / 前缀不是插件 mod id | 警告 + 永不触发的 handle |
| 重复 id（同一插件） | 幂等返回已有 handle + 警告 |
| 重复 id（跨插件） | 先注册者胜；后来者拿到**永不触发的 handle** + 警告（不把别人的 handle 交出去） |
| `targetClass` 但未声明 SCREEN / OVERLAY | 警告 + 忽略该过滤 |
| 声明了 SCREEN/OVERLAY 但 `targetClass` 指向的类不存在 | 允许（类可能后加载）；绑定休眠 |
| 默认绑定按钮未知 | 警告 + 跳过该默认值 |
| 回调结束后再调用 registrar | 抛 `IllegalStateException`（编程错误，不静默） |

## 5. 生命周期

`ControlFlexApiAdapter.discoverPlugins()` 现有顺序：

```
onInstallCompatConfigs
onInstallGuideAssets
cameraLookConsumer
onControlFlexReady
```

改为：

```
onInstallCompatConfigs
onInstallGuideAssets
cameraLookConsumer
onRegisterActions(registrar)   // 新增：registrar 仅此期间有效
onControlFlexReady
```

注册必须早于 `bindingRegistry.loadFromJson`，默认值才有机会参与解析（与 vehicles §4.3 同一时序要求）。旧桥不实现新回调时行为不变（default method）。

## 6. 存储与解析

### 6.1 Screen / Overlay：沿用现有的每屏表

不新增文件。已注册动作的每屏绑定写进**每屏编辑器本来就在用的那张表**：

`config/controlflex/compat/user_screen_overrides.json` → `screens.<fqn>.<actionId>`（Overlay 在 `overlays`）。

```json
{
  "version": 1,
  "screens": {
    "com.cobblemon.mod.common.client.gui.battle.BattleGUI": {
      "cfx_compat_cobblemon:battle_prev": "leftBumper|PRESS",
      "cfx_compat_cobblemon:battle_fight": ""
    }
  }
}
```

- 访问器沿用 `UserScreenOverrideStore`（`contains` / `get` / `put` / `removeAction` / `save`）。
- 只存**用户改过的值**，包括显式 `""`（表示“我就是要它空着”）；注册默认值只在内存里兜底，首次运行不落盘，没有迁移脚本。
- 解析顺序：`contains` 为真 → 用它（`""` = 未绑定）；否则注册默认；否则未绑定。
- 键是否存在必须用 `contains` 判断，不能用“解析结果为空”反推，否则清空会被当成未配置、默认值又回来了。
- 写入后调用 `save()`；`save()` 顺带失效前台解析缓存，编辑器与提示在同一帧看到新值。

**为什么不是第二个 store（初版曾这样做，实机暴露为 bug）**：每屏编辑器（`EntryConfigScreen`）读写的始终是这张表。再引入 `screen_action_bindings.json` 会让“保存成功、界面显示正确、运行时解析不到”同时成立——解析器只读新文件，且在旧的前台覆盖解析之前就返回空绑定，编辑器的值永远没有机会参与。一个概念一张表，才不会有两条真相。

**为什么也不是 compat 的 `screen.<fqn>.bindings`**：那要求模组必须有一份 compat 文件并维护 `screenKeys` 段；cobblemon 迁移后只保留分组元数据，动作归属不再经过 compat。`user_screen_overrides.json` 与 compat 文件解耦，正合适。

### 6.2 解析入口：复用 `getEffectiveBinding`

已注册动作不新增第二套解析调用链，而是接进现有的唯一收口 `BindingRegistry.getEffectiveBinding(actionId, layer)`：

```
getEffectiveBinding(id, layer):
  若 id 未注册 → 现有逻辑（成员门 → 每屏表 → 层映射）
  若 id 已注册：
    IN_GAME  → 沿用层映射（默认值已种入，见 §6.3）
    SCREEN   → 当前 Screen 类命中作用域 ? store/默认 : unbound
    OVERLAY  → 当前 Overlay 类命中作用域 ? store/默认 : unbound
```

这样以下路径**不需要各写一遍**：组合构建、`isSuppressedBinding`、`DoubleHoldInterrupt`、GUI/游戏事件汇、`IActionBindingProvider`、提示与派发的一致性。解析器本身仍是独立类（`RegisteredActionResolver`），只是由 `getEffectiveBinding` 调用。

配套约束：

- `BindingRegistry.getActionIds()` 合并已注册 id（`cachedActionIds` 失效），使游戏 / Overlay 相位的全量遍历能见到它们。
- `BindingSerializer` 的两处层加载 / 导出**显式跳过**已注册 id：它们不属于 `mainActionBindings` / `guiActionBindings` / `overlayActionBindings`，序列化时若照常处理会重新制造全局映射条目。
- `isActionRelevantForLayer` 对已注册动作返回“声明的层”，让 UI 认为它可编辑；序列化的跳过由上一条独立保证，不靠相关性。
- `ActionCandidacy`：已注册动作的 GUI 候选资格 = 作用域命中 + 解析结果已绑定，不再查映射表成员。

Screen 与 Overlay 的类名分别来自 `MCCompat.getScreen(mc)` 与 `ScreenHistory.currentOverlayClass()`，与 `IScreenHandler` 的目标解析同源。

### 6.5 注册表接缝（实现时必须逐条对齐）

| 接缝 | 行为 |
|---|---|
| `getActionIds()` | 合并已注册 id；注册时置空 `cachedActionIds`（UI 行缓存用同一个 identity token） |
| `isActionRelevantForLayer` | 已注册动作只对自己声明的层返回 true：`IN_GAME → main/shift1..4`、`SCREEN → gui`、`OVERLAY → overlay`。**必须排在“含冒号 = 全层可配”分支之前**，否则屏幕动作会被当成全层动作 |
| `BindingSerializer` 层加载 / 导出 | 显式跳过已注册 id，避免它们被写进 `guiActionBindings` / `overlayActionBindings`（全局暴露的回潮） |
| `shiftRelevantActionIds()` | 由上一行自然满足：SCREEN 动作对 `shift1` 不相关，不需要模板补空条目，`TemplateV2ConformanceTest` 不受影响 |
| `getActionDisplayName` / `getActionLocalizedName` | 已注册动作读 `nameKey`，缺失时回退 actionId |
| UI 分组 | 新增 `getActionGroupModId(id)`：已注册动作返回 `groupModId`，其余回退现有 `dynamicActionModId` |
| `setActionBinding(id, binding, GUI/OVERLAY)` | 已注册动作写入每屏表（与编辑器同一张），不写层映射（UI 捕获键位的唯一写入口） |
| `resetActionBinding(id, GUI/OVERLAY)` | 已注册动作删除每屏表的键（回落默认），不写 `""` |
| 测试隔离 | `RegisteredActionRegistry` 是进程级单例，提供包内可见的 `clearForTests()`；注册窗口关闭后清空不影响生产 |

### 6.3 IN_GAME：沿用层映射

与 vehicles 方案一致：缺键时把默认绑定种入声明的每个游戏层；profile 里已有值（含 `""`）时尊重现值。默认值只改内存，保存 profile 时自然落盘。

`IN_GAME` 在 v1 展开为 `main + shift1..4` 五个层，同一条默认值写进每一层（否则玩家按住 shift 层时动作消失）。逐层不同默认值不在本次范围（§17）。

### 6.4 与内置 / 旧动态动作的关系

三者共用同一张每屏表（§6.1），差别只在**候选资格**与**默认值来源**：

| 动作类别 | 每屏绑定来源 | 候选资格 | 默认值 |
|---|---|---|---|
| 已注册动作 | `user_screen_overrides.json` | 注册声明的 layer + class | 注册项里的 `default` |
| 内置动作（含 `hud_toggle`） | 同上 + compat 的 screen entries | 层映射成员 | `DefaultBindings` / 模板 |
| 旧动态动作（compat JSON 的 `screenKeys`） | 同上 | 层映射成员 | compat 配置 |

因为表只有一张，编辑器不需要合并两个数据源，也不存在“哪边优先”的问题。把内置动作也迁到注册机制属于后续工作，不在本次范围（§15）。

## 7. 派发

候选构造：

| 相位 | 候选来源 |
|---|---|
| SCREEN | 既有 `guiActionBindings` 遍历 **+** 追加“作用域命中当前 Screen 的已注册动作” |
| OVERLAY | 既有全量 id 遍历（`getActionIds` 已含注册动作），解析时按当前 Overlay 类过滤 |
| GAME | 同上；`IN_GAME` 动作解析走层映射，其余解析为未绑定 |

- 已注册的 Screen/Overlay 动作**跳过** `isGUIAction` 的类别判断：作用域即候选资格。
- 绑定解析走 §6.2 的 `getEffectiveBinding`；未绑定 = 不参与组合，不产生回调。
- 不额外构造 `ActionBinding`：`getEffectiveBinding` 已经返回它，后续组合构建、抑制、状态跟踪都复用现有路径。
- 回调挂在**组合事件汇**（`EventSink`）上，不是每 tick 的 `handleGUIAction`：后者对 HOLD 会每 tick 调用一次，挂在那里就会重现 `hud_toggle` 每秒翻 20 次的问题。按模式取事件：

| 模式 | `onActionTriggered` 取的事件 | `onActionReleased` 取的事件 |
|---|---|---|
| `PRESS` / `DELAY_PRESS` / `LONG_PRESS` | `PRESS` / `DELAY` / `LONG_PRESS` | — |
| `RELEASE` / `TAP` / `DOUBLE_TAP` | `RELEASE` / `TAP` / `DOUBLE_TAP` | — |
| `DOUBLE_PRESS` | `DOUBLE_PRESS` | — |
| `HOLD` / `DELAY_HOLD` / `LONG_HOLD` / `DOUBLE_HOLD` | 首次激活事件（`PRESS` / `DELAY` / `LONG_PRESS`） | `RELEASE` 或 `DESTROY` |
| `TOGGLE` / `DOUBLE_TOGGLE` | 切到 on 的那次 | 切到 off 的那次 |

- 游戏层与 GUI 层各有一套组合管理器，但事件形状一致；回调实现挂到两者共用的出口，避免相位间行为分叉。
- 同时存在 Screen 与 Overlay 时沿用现有优先级：Screen 优先。
- 一个动作只应收到一次回调：即使它同时命中多条绑定（例如组合键与单键冲突），以实际激活的组合为准。

`ActionCandidacy` 相应调整：

```
内置 / 旧动态动作：维持现状（层映射成员 + 类别）
已注册动作：scope 命中 + 解析结果已绑定
```

已注册动作**绕过** `LayerMembershipStore`：它们没有“按层启用 / 停用”的概念，也不出现在成员开关 UI 里。

## 8. 与现有 API 的关系

| 能力 | 变化 |
|---|---|
| `IScreenHandler` / Registry | 不变：返回键、HUD 状态仍按屏幕类注册 |
| `IActionBindingProvider` | 解析路径按 action kind 分派；已注册动作返回每屏表/默认值，提示与实际派发一致 |
| 内置 `hud_toggle` | 不变；不新增 cobblemon 专属开关动作，也不给 GUIDE 默认 |
| `IInteractiveContextRegistrar` | 不变（Overlay 前台登记；Overlay 动作用它报告的类名匹配） |
| `IActionStateProvider` | 不变；新动作的 `isActive()` 由注册表自己维护（HOLD 类走活动集，一次性模式走最后触发 tick） |

## 9. UI

- 「模组适配 → `<groupModId>`」列出该模组注册的动作；Screen 动作进入对应屏幕的每屏绑定编辑器。
- 已注册动作**不出现**在全局 GUI / Overlay 层动作列表里（它们不在那些映射表中）。
- 每屏编辑器与解析器读写同一张表（§6.1），不存在“UI 显示 A、运行时用 B”的分叉。
- 编辑器需要显示来源：`用户` / `默认`。提供两个不同操作：
  - **恢复默认**：删除每屏表里的键（回落到注册默认）。
  - **清空**：写入 `""`（永久空，不再回落）。
- 冲突检测沿用现有逻辑：同一作用域、同一按钮组合、同一触发模式视为冲突；默认值与 `move_forward` 这类共用摇杆的情况只提示不阻止（与 vehicles D4 一致）。

## 10. 核心实现映射

新增 / 修改（名称以实现时为准）：

| 位置 | 内容 |
|---|---|
| `api/ActionLayer` `ActionRegistration` `DefaultBinding` `IActionRegistrar` `IControlFlexAction` `ActionTrigger` | 新类型 |
| `api/IControlFlexPlugin` | 三个 default 回调 |
| `api/ControlFlexApi` | 无需新访问器（registrar 只经插件回调发放） |
| `compat/RegisteredActionRegistry` | definitions + handles + 归属插件；开启 / 关闭注册窗口；按类查作用域 |
| `compat/RegisteredActionResolver` | 每屏表/默认解析与写入（`storeBinding` / `clearBinding`）；由 `getEffectiveBinding` 调用，供派发 / provider / UI 共用 |
| `compat/UserScreenOverrideStore` | 每屏绑定的唯一存储，编辑器与注册动作解析共用 |
| `binding/BindingRegistry` | `getEffectiveBinding` 分流（§6.2）；`getActionIds` 合并注册 id 并失效缓存；本地化名 / 分组查询 |
| `binding/BindingSerializer` | 两处层加载 / 导出声明显式跳过已注册 id |
| `binding/BindingMapper` | GUI 候选追加“作用域命中当前 Screen 的已注册动作”；游戏 / Overlay 沿用全量 id 遍历 |
| 组合事件出口（`EventSink` 消费处，游戏与 GUI 共用） | 按 §7 的事件映射发出 `onActionTriggered` / `onActionReleased`；**不**挂在每 tick 的 `handleGUIAction` |
| `gui/GuiActionHandler` | 入口分支：已注册动作 → 插件回调并 return；其余走现有 switch |
| `binding/ActionDispatcher` | 入口分支：已注册的 IN_GAME 动作 → 插件回调，不再注入键 |
| `binding/ActionCandidacy` | 已注册动作按 scope 判定 |
| `compat/ActionBindingProviderImpl` | 解析分派（已注册 → resolver；其余 → 现状） |
| `compat/ControlFlexApiAdapter.discoverPlugins` | 插入 `onRegisterActions` |
| UI 每屏绑定编辑器 / 模组适配面板 | 列出注册动作、来源与恢复默认 |

## 11. cobblemon 迁移

| 项 | 改动 |
|---|---|
| 18 个动作 | id 改为 `cfx_compat_cobblemon:battle_*`；`layers={SCREEN}`，`classes={BattleGUI}` |
| 默认绑定 | `battle_prev=leftBumper|PRESS`、`battle_next=rightBumper|PRESS`、`battle_log_up=rightStickUp|PRESS`、`battle_log_down=rightStickDown|PRESS`；其余 14 个留空 |
| 删除 | 伪 `KeyMapping` 注册、`BattleKeys`、compat JSON 的 `screenKeys` 段、`candidateIds()`、ticker 里的 `consumeClick` |
| 保留 | `BattleScreen`（反射点击）、`BattleSelection`、`BattleHud`、`BattleScreenHandler`；compat JSON 只保留 `mod_id` / `mod_name` / `home_page` 这类分组元数据 |
| 触发 | 回调里按 actionId 分派到现有逻辑（点击格子 / 移动高亮 / 滚动日志） |
| 旧绑定 | 旧 id（`cobblemon:key.cfx_compat_cobblemon.*`）作废，玩家重绑；未发布，可接受 |

日志滚动默认用 PRESS（每按一次滚固定行数）；若后续需要“按住连续滚”，需要先定义重复间隔语义（§15）。

## 12. vehicles 兼容

vehicles 的四个动作按 `layers={IN_GAME}` 注册，默认绑定种入 main + shift1..4；桥接通过 `isActive()` 读电平。与本设计同一套注册项，只是不带 `classes`。

## 13. 版本与发布顺序

1. `control-flex-api` 0.14.0（新增类型 + 插件回调）。
2. `control-flex` 核心实现，`cfx_api_version` 抬到 0.14.0。
3. `cfx-compat-cobblemon` / `cfx-compat-vehicles` 分别迁移。

0.13.0 未发布，直接演进到 0.14.0，不做兼容层。

## 14. 测试

核心：

- 注册校验：命名空间、重复 id、`targetClass` 与 layer 的组合、未知按钮、窗口关闭后再注册。
- 默认解析矩阵：无用户值 → 默认；`""` → 未绑定；有用户值 → 用户值；Screen / Overlay / GAME 三种归属分别验证。
- store round-trip：`""` 必须保留为键（重启后仍是空，不回落默认）。
- 候选：另一个 Screen 类不派发；同类的已注册动作参与组合；未绑定不参与；旧动作路径不受影响。
- 回调：PRESS 一次；TAP 在抬起；HOLD 触发 + 释放各一次；TOGGLE 每次翻转各一次；异常隔离。
- `isActive()`：HOLD 按住 true、释放 false；一次性模式仅触发 tick；未绑定恒 false。
- 提示查询：`getActiveBinding` 对已注册动作返回 store/默认值；上下文不匹配时未绑定。
- Provider / Candidacy / 派发三处对同一动作给出一致结论。

桥接：

- 18 个动作都能通过回调触发对应点击；高亮 / 光标 / 日志行为不变。
- 默认 LB/RB、右摇杆上下开箱可用；恢复默认与清空的行为可区分。

## 15. 失败模式与边界

| 场景 | 行为 |
|---|---|
| 注册回调抛异常 | 隔离该插件，其余插件继续注册；记录堆栈 |
| 动作回调抛异常 | 记录 + 继续派发其他动作 |
| store 损坏 | 视为空 + 警告；不覆盖原文件，直到下一次用户修改 |
| 插件被卸载 / 未加载 | 动作不再注册，store 条目保留（休眠）；重装后绑定回来 |
| 两个插件抢同一 id | 先注册者胜，后来者警告 |
| 同一动作同时命中 Screen 与 Overlay | Screen 优先（沿用现有相位优先级） |
| 玩家清空后重装模组 | store 里是 `""`，保持空 |
| 类改名 / 屏幕类消失 | 绑定留在旧类名下；新类名下按默认值 |

## 16. 决策记录

| # | 决定 | 理由 |
|---|---|---|
| D1 | 注册动作不创建 `KeyMapping` | 目标就是不再向 MC 全局暴露 |
| D2 | Screen/Overlay 动作不进全局映射表 | 候选资格改由作用域决定，消除“表里没键就不派发” |
| D3 | 一条默认绑定应用到声明的所有层/类 | 当前两个消费者都不需要逐层不同值；需要时再加 map |
| D4 | `hud_toggle` 保持内置，不给 GUIDE 默认，不新增专属动作 | 它是通用能力，不该按模组复制 |
| D5 | 回调在触发时发出，HOLD / TOGGLE 另有释放回调 | 桥接不必轮询，也不丢一次按下 |
| D6 | 解析优先级：用户 store > 注册默认 | 用户的清空必须永久生效 |
| D7 | 类过滤精确匹配、不继承 | 与 `IScreenHandler` 一致，限制爆炸半径 |
| D8 | 类名未加载时绑定保留、动作休眠 | 与 vehicles “IV 未安装仍注册”一致 |
| D9 | 内置与旧动态动作维持原路径 | 迁移可以按桥逐个进行，不需要一次性切换 |
| D10 | 回调按边沿发出，HOLD 不随每 tick 重复 | 避免 `hud_toggle` 绑 HOLD 时的 20 次/秒问题重演 |
| D11 | 已注册动作绕过层成员开关 | 它们没有“按层启用”的语义，避免制造无意义的开关 |
| D12 | 每屏表只存用户值，默认值不落盘 | 无迁移脚本；恢复默认 = 删键，清空 = 写 `""` |
| D13 | 已注册动作接进 `getEffectiveBinding` 单一收口，而不是各调用点各写一套 | 抑制、双按、释放、提示查询全部复用现有路径，行为不会分叉 |
| D14 | 相关性（UI 可见）与序列化（是否落盘）拆开判断 | 屏幕动作要对 UI 显示“可编辑”，但绝不能进全局映射表 |
| D15 | 已注册动作复用 `user_screen_overrides.json`，不另建 store | 每屏编辑器是既有写入口；第二个 store 会让“保存成功、提示不出现”同时成立（实机已发生） |

## 17. 开放问题

1. 逐层不同的默认绑定（`Map<ActionLayer, DefaultBinding>`）是否需要在 0.14.0 就提供？当前判断：不需要。
2. 已注册的 Screen 动作是否也要出现在“主绑定列表”（非每屏编辑器）？当前判断：不出现，避免制造第二处真相。
3. 内置动作的每屏绑定是否也迁到新 store（统一存储、去掉 compat 依赖）？当前判断：不在本次范围。
4. 日志滚动若改 HOLD，需要先定义重复间隔语义；不在本次范围。
