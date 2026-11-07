# 动作绑定查询 API 设计（Action Binding Query）

**状态：** 接口已落地（两轮自审后修订）；主仓库实现待做
**分支：** 待建（建议 `feat/action-binding-query`，从 `main` 拉出）
**目标版本：** `0.10.0`
**日期：** 2026-11-06

---

## 1. 背景

桥接模组要在游戏画面里自己画「这个动作现在按哪个键」的提示。典型场景：Cobblemon 战斗界面把 4 个选项块映射成 4 个动作，桥接模组要在每块旁边标出玩家当前绑定的手柄键图标。

现在的 API 做不到这件事，缺两块数据：

1. **动作绑了哪个键。** `IActionStateProvider.isGameActionActive()` 只能查"是否正在触发"，`IInputProvider.getControllerState()` 只能查硬件的绝对状态。没有"`actionId` → 绑定按键"的查询。
2. **该画什么图标。** 图标不在 API 里，而在主仓库的私有实现中（`ControllerGlyphRenderer`），且是**图集**而非单文件：`controlflex:textures/gui/controller/controller_{xbox|playstation|switch}.png`，512×128、16 列、每格 32×32，格子序号来自 `controllers/glyph_mappings/<skin>.json`。

桥接模组若自行维护一套图标，会和玩家实际用的皮肤（Xbox / PS / Switch，含 DualSense 判定）不一致；若自行猜按键，改键后就错。两者都必须由 ControlFlex 给出。

## 2. 目标 / 非目标

**目标**

- 一次调用拿到"某个动作现在绑了什么、该画什么"，且自洽（不会出现按键来自一种皮肤、图标来自另一种）。
- 接入方不需要知道图集布局、皮肤选择、shift 组合、键盘键退化这些内部概念。
- 只读、无副作用、可在渲染阶段每帧调用。

**非目标**

- 不做"用代码改绑定"（写入是配置界面的事）。
- 不做"查询任意 Screen 的绑定表"（只能查当前生效上下文，见 3.3）。
- 不提供绘制方法（API 没有 Minecraft 依赖，见 3.1）。

**反目标**

- 让接入方缓存查询结果。绑定会在改键、换手柄、开关 Screen 时变化，缓存必然过期。
- 把"动作当前是否被状态抑制/是否可触发"塞进绑定信息。那是 `IPlayerStateRegistry` 的读取侧，另一件事。

## 3. 约束

### 3.1 无 Minecraft 依赖

`control-flex-api` 是独立 `java-library`，`--release 8`，不依赖 Minecraft。因此：

- 不能出现 `GuiGraphics`、`ResourceLocation`、`Component` 等类型。
- 纹理只能用字符串 ID（`"controlflex:textures/gui/controller/controller_xbox.png"`）表达。
- **不可能提供 `drawAt(GuiGraphics, ...)` 这类便捷方法**——这是本节最容易被忽略的后果。

### 3.2 线程与生命周期

仅客户端主线程、渲染阶段可调用。返回值是当次解析的快照，跨帧不可复用。

### 3.3 上下文即"当前生效"

主仓库的 `BindingRegistry.getEffectiveBinding(actionId, layer)` 内部已走 `ForegroundBindingResolver`，会叠加当前前台 Screen 的 per-FQN 覆盖（`user_screen_overrides.json`）。所以 API 不需要 `Screen` 参数，也不需要接入方传 FQN。

唯一要接入方接受的是：答案随当前相位（GAME / OVERLAY / GUI）与当前 shift 层变化，无法在"打开界面前"预知。这是刻意选择——见 5.2，宁可只回答"此刻"，也不提供会与派发分裂的第二种语义。

### 3.4 向后兼容

`package-info` 已承诺"新增方法以 `default` 形式加入"。本设计**不改动现有接口**，只新增接口与枚举，属 minor 版本。

## 4. 设计决策

| 决策 | 选择 | 理由 |
| --- | --- | --- |
| 接口数量 | **一个** `IActionBindingProvider`，返回一个聚合对象 | 按键与图标是同一次解析的两个投影；分两次查会在中间发生皮肤/绑定变化时产生"按键与图标不一致"的撕裂 |
| 纹理表达 | **矩形区块**：`textureId + u/v/w/h + texWidth/texHeight` | 不暴露"图集"概念。将来若改成单文件小图，只需把 `u/v` 置 0、`w/h` 置图标边长，API 不变 |
| 未绑定 | 返回**非 null** 的信息对象，用 `isBound()` 表达 | 省掉调用方判空；与现有"provider 可空、结果不为空"的习惯一致 |
| 图标缺失 | `getTextFallback()` 给出可绘制文本（`"F1"`、`"HOME"`） | 键盘键与未知硬件没有图集格子，必须仍能画出来 |
| 层 | **单一方法** `getActiveBinding`，内部走派发同一套上下文判定 | 最初设想"GUI 层 / 游戏层两个方法"是错的：实际是三相位（`InputPhase` = GAME / OVERLAY / GUI），且 GAME 相位用的是 `getActiveLayer()`，可能是 shiftN。拆两个方法必然有一半答案是错的，见 5.2 |
| shift 组合 | **扁平**进 `getButtons()` / `getGlyphs()`，修饰键排在最前 | 与 `ResolvedGuideBinding.glyphButtonIds()` 一致；结构化拆成 `getModifier()` 会造成"显示名含修饰键、图标不含"的错位 |
| 图层常量 | 新增 `BindingLayer` 字符串常量类 | 与 `ButtonName` 同一风格；避免消费方自己拼 `"shift1"` |
| 触发模式 | 镜像一个 `TriggerMode` 枚举 | 与 `InputMode`、`ControllerType` 的既有做法一致；按名称映射，未知值回落 `PRESS` |
| 是否附带单文件小图 | **不做** | 矩形区块已把图集细节挡住；多一套资源就要多一套生成脚本与皮肤同步 |
| 是否暴露 `isFromScreenOverride()` | **不做** | HUD 用不到；需要它的场景是配置界面排错，届时应开独立查询 |

## 5. API 形态

### 5.1 入口

```java
// ControlFlexApi
public static IActionBindingProvider getActionBindingProvider();   // 未安装时为 null
```

### 5.2 查询

```java
public interface IActionBindingProvider {

    /**
     * 在当前生效的输入上下文里解析 actionId。
     * 上下文判定与派发器一致，返回值永不为 null。
     */
    IActionBindingInfo getActiveBinding(String actionId);
}
```

**为什么只有一个方法。** 最初设计成 `getGuiBinding` / `getGameBinding` 两个方法，自审时发现前提就错了：ControlFlex 的输入不是"两层"，而是三相位。

```java
enum InputPhase { GAME, OVERLAY, GUI }          // binding/InputPhase.java

// binding/PhaseTransition.java:23
BindingContext.of(phase, activeLayer) →
    GAME    → activeLayer（main 或 shift1..4）
    OVERLAY → LAYER_OVERLAY
    GUI     → LAYER_GUI
```

相位由 `BindingMapper.resolveTargetPhase()` 决定，优先级是 **Screen 打开 → GUI**、**前台交互 overlay → OVERLAY**、否则 GAME。所以：

- 桥接模组把战斗界面做成普通 Screen 时，答案是 `gui`；
- 做成 `IInteractiveContextRegistrar` 注册的交互式 overlay 时，答案是 `overlay`——这正是原设计声明"v1 不做"却又是最自然的落地方式；
- 战斗最小化成 HUD 时，答案取决于当前 shift 层，`getGameBinding(main+auto-shift)` 会给出错的一层。

一个方法把这三种情况都覆盖掉，也不再有"提示值 vs 实际派发值"的分裂。

### 5.3 结果

```java
public interface IActionBindingInfo {

    String getActionId();

    boolean isBound();

    /**
     * 绑定的按键 ID，顺序即组合顺序；未绑定时为空列表。
     * 绑在 shift 层时修饰键排在最前，例如 {@code ["leftBumper", "buttonX"]}。
     */
    List<String> getButtons();

    /** "A" / "LB + X" / "F1"，已按手柄皮肤本地化。未绑定时为空串。 */
    String getDisplayName();

    /** 与 getButtons() 等长同序；每个按键都能画出东西（图形或文本）。 */
    List<IControllerGlyph> getGlyphs();

    /** 本次查询所咨询的层（见 BindingLayer）；未绑定也照实返回。 */
    String getLayer();

    TriggerMode getTriggerMode();
}
```

未绑定时的取值约定（不抛异常、不返回 null）：`getButtons()` / `getGlyphs()` 为空列表，`getDisplayName()` 为空串，`getLayer()` 仍给出被咨询的层（永不为空），`getTriggerMode()` 为 `PRESS`，`getActionId()` 把 `null` 归一为空串。`actionId` 传 `null` 或空串同样返回未绑定 info。

**shift 组合是扁平的，不单独返回修饰键。** 这与主仓库既有做法一致：`ResolvedGuideBinding.glyphButtonIds()` 就是把修饰键按钮放在动作按钮之前拼成一串。不设 `getModifier()`，因为"`getDisplayName()` 含修饰键、`getGlyphs()` 不含"只能满足一条，接入方几乎必然把修饰键画两遍或一遍不画。需要知道"这是不是 shift 组合"时读 `getLayer()`。

### 5.4 图标

```java
public interface IControllerGlyph {

    /** 纹理资源 ID，形如 "controlflex:textures/gui/controller/controller_xbox.png"。 */
    String getTextureId();

    int getU();
    int getV();
    int getWidth();
    int getHeight();

    int getTextureWidth();
    int getTextureHeight();

    /** 无图集格子时非空（键盘键、未知手柄），画它代替图标。 */
    @Nullable String getTextFallback();
}
```

### 5.5 绘制（接入方侧）

```java
IControllerGlyph g = info.getGlyphs().get(0);
if (g.getTextFallback() != null) {
    guiGraphics.drawString(font, g.getTextFallback(), x, y, 0xFFFFFFFF, false);
} else {
    ResourceLocation tex = ResourceLocation.parse(g.getTextureId());   // 1.20.x 用 new ResourceLocation(...)
    guiGraphics.blit(tex, x, y, size, size,
            g.getU(), g.getV(), g.getWidth(), g.getHeight(),
            g.getTextureWidth(), g.getTextureHeight());
}
```

组合绑定（`LB + X`）由接入方在相邻 glyph 之间插入 `"+"` 文本，宽度取 `font.width("+")`——与 `ControllerGlyphRenderer` 的内部做法一致。

### 5.6 触发模式枚举

镜像主仓库 `com.ifels.controlflex.binding.TriggerMode` 的 13 个值：
`PRESS / RELEASE / HOLD / TAP / DELAY_PRESS / DELAY_HOLD / LONG_PRESS / LONG_HOLD / DOUBLE_PRESS / DOUBLE_TAP / DOUBLE_HOLD / DOUBLE_TOGGLE / TOGGLE`。

实现侧按 `name()` 映射；出现未识别名称（未来新增）时回落 `PRESS`，与 `TriggerMode.fromName` 的容错一致。

## 6. 主仓库实现要点

| 环节 | 复用点 | 备注 |
| --- | --- | --- |
| 上下文层 | 现成材料：`Minecraft.screen`、`BindingMapper.isOverlayPhaseActive()`、`BindingRegistry.getActiveLayer()` | 顺序照抄 `resolveTargetPhase()` 的优先级：Screen → `gui`；否则 overlay 相位 → `overlay`；否则 `activeLayer`。**不要读 `BindingMapper.phase` 字段**（无 getter，且比 Screen 开放晚一拍，会闪一帧错误的键） |
| 取绑定 | `BindingRegistry.getEffectiveBinding(actionId, ctx.layer())` | 已含 `ForegroundBindingResolver` 与 per-Screen 覆盖，**不要再自己拼** |
| shift 修饰键 | `getEffectiveBinding(ActionId.LAYER_SHIFTn, LAYER_MAIN)` | 只在 `ctx.layer()` 是 shiftN 且该动作有绑定时，把修饰键按钮前置；不要用 `GuideBindingResolver` 的 AUTO——它拿"shift 与 main 是否相同"当判据，与派发语义不同 |
| 皮肤 | `GuideGlyphSkinResolver.resolveCached(...)` | 缓存已存在，API 侧不再加一层；**必须与显示名共用同一个 skin 值**，见 6.1 |
| 键 → 格子 | `ControllerGlyphMapper.resolveChar(skin, buttonId)` / `hasGlyph(...)` | |
| 格子 → 矩形 | `ControllerGlyphRenderer` 的 `ATLAS_CELL=32`、`ATLAS_COLS=16`、`ATLAS_W=512`、`ATLAS_H=128` | 这些目前是 private，需要提成包内可见常量或抽一个 `atlasRect(slot)` 静态方法 |
| 显示名 | `ButtonDisplayNames.shortName(skin, buttonId, isPS5)` 逐键拼接 | **不要用 `ActionBinding.getButtonsDisplayString()`**：它内部读静态 `GuideGlyphSkinAccess.current()`，与图标那次解析之间可能已经换过手柄 |
| 键盘键回退 | `BindingKeyboardIds.keyboardDisplayLabel(buttonId)` | 与 `ControllerGlyphRenderer.textFallbackLabel` 同一判据；返回 null 时再退到原始 buttonId，保证"一定能画" |

### 6.1 皮肤必须单点取值

一次查询里，显示名和图标格子必须来自同一个 `GuideGlyphSkin` 值，否则 §2 承诺的"按键与图标不会来自两种皮肤"当场失效。两个坑：

- `GuideManager.getCurrentGlyphSkin()` 会走 `persistLastSkinIfNeeded` 写 `cfx-client.json`，**不能在每帧调用**。
- `GuideGlyphSkinResolver` 是实例级缓存，实现自己 new 一个会拿到与全局不同的 `lastResolved`。

做法：实现类持有一个 resolver 实例，与 `GuideManager` 使用同一个 `gamepadSelector` 与 `glyphStyle` 来源；若 `GuideManager` 没有无副作用的只读访问器，给它加一个 `peekGlyphSkin()`（纯读，不写盘），API 实现统一从这里取。

新增实现类放在 `com.ifels.controlflex.compat`（或新包 `com.ifels.controlflex.api.impl`），类名须以 `com.ifels.` 开头——`ControlFlexApi` 的注入校验会检查包名。

注入时机与现有 provider 一致：客户端初始化阶段 `ControlFlexApi.setActionBindingProvider(...)`；未注入时 `isAvailable()` 不受影响（保持现有语义：只检查三个旧 provider）。

## 7. 版本与发布

1. `control-flex-api/gradle.properties`：`api_version` `0.9.2` → `0.10.0`。
2. `./tools/publish_maven_local.sh` 发布到 `~/.m2`，坐标为 `com.github.ControlFlexMC:control-flex-api:0.10.0`（脚本会校验 POM 不是 JitPack wrapper）。
3. 消费方依赖坐标写死 `0.10.0`。主仓库 `build.gradle.kts:103` 已有 `mavenLocal()`（就是为本地联调准备的），无需改动仓库列表。
4. 主仓库 `gradle.properties` 的 `cfx_api_version` 同步到 `0.10.0`。
5. 公共文档（`docs/zh`、`docs/en` 的 `api-reference.md`）在实现合并时补章节，本 spec 不进公共文档。

> **反复发布的窗口期。** `0.10.0` 在评审过程中已就地重发过一次（接口从"两个方法"改成"一个方法"）。该版本未打 tag、无外部消费者，重发是安全的；一旦发到 JitPack，后续任何接口改动都必须走 `0.10.1`。

## 8. 验收

本 spec 只交付 API 与主仓库实现，**不交付任何具体模组的动作 id**。Cobblemon 那 4 个 `/招式` 动作来自桥接模组自带的 `KeyMapping` 与 compat 配置，属另一个计划；手测用仓库里已有的 compat（`epicfight` / `jei`）即可。

- 单元测试：未绑定 / 单键 / 多键组合 / shift 修饰四种形态；`getGlyphs()` 与 `getButtons()` 等长同序；shift 形态下修饰键在首位。
- 单元测试：键盘键绑定返回 `textFallback` 非空；`getGlyphs()` 仍与 `getButtons()` 等长。
- 单元测试：同一动作在 `gui` 与 `main` 上下文下返回不同绑定（层隔离）；构造 OVERLAY 相位时走 `overlay` 层。
- 单元测试：shift 层修饰键未绑定时不前置；绑定时前置。
- 单元测试：`null` / 空 `actionId` 返回未绑定 info，且 `getLayer()` 仍给出被咨询的层。
- 手测：给 `epicfight` 的某个 GUI 层动作临时绑一个键，`getGlyphs()` 返回的矩形能在游戏里 blit 出正确图标。
- 手测：把皮肤切到 PlayStation 后重查，图标变为 ✕○□△，且 `getDisplayName()` 同步变化（验证 6.1 的单点取值）。
- 手测：改键后**下一帧**结果即变（验证没有跨帧缓存）。

## 9. 留白与已知偏差

- **shift 锁存与按住不做区分。** 层为 shiftN 时，`getButtons()` 把修饰键前置，表达的是"按住修饰键再按这个键"。但 shift 层可以被锁存（按一次保持），锁存状态下单独按该键就有效，HUD 仍会显示两段。缺的是"当前 shift 是锁存还是按住"这个状态，v1 不查。
- **答案随相位漂移。** 相位判定带 3 tick 去抖（`overlayEnterStreak` / `overlayExitStreak`），所以刚关掉一个 overlay 的三 tick 内，查询可能仍报告 `overlay` 层。这是与派发一致的代价，不是 bug。
- 动作名的本地化标签（"跳跃"）不在本设计内。`IActionBindingInfo` 只回答"按哪个键、画什么"，不回答"这个动作叫什么"。
- **API 不回答"这个动作属不属于当前界面"。** 若动作绑在全局 GUI 层，任何界面查询都会返回已绑定。要限定只在某个界面生效，靠 per-Screen 覆盖（`user_screen_overrides.json`）把绑定写在那一个 FQN 下，而不是靠 API 过滤。
- 单文件图标资源（构建期从图集切分）暂不做，理由见决策表。
- `IInteractiveContextRegistrar` 注册的 overlay 现在被 `getActiveBinding` 覆盖，但"某个 overlay 是否在前台"本身不对外暴露；接入方要自己判断该不该画提示。

## 10. 待确认

这两条由仓库维护者决定，影响计划范围而非 API 形态：

1. **回填范围。** 工作区里有 18 个版本树，只有 `control-flex-1.20.1/api/` 带一份 vendored API 源码，其余（含 1.21.1、stonecutter 的 neoforge/fabric/1.21.11/26.x 变体）通过 JitPack 依赖。本计划只改 `control-flex-api` 与 1.21.1 主仓库，还是也要同步 1.20.1 的那份副本？
2. **发布通道。** 0.10.0 只发 mavenLocal（本机验证用），还是同时打 tag 发 JitPack？第三方桥接模组按 `0.10.0` 写死坐标后，只有 JitPack 上有该版本才能编译。

## 11. 修订记录

**第一轮自审（对照派发实现）—— 推翻了层模型**

- 原设计"GUI 层 / 游戏层两个方法"前提错误：实际是三相位（`InputPhase` = GAME / OVERLAY / GUI），且 GAME 相位用的是 `getActiveLayer()`（可能是 shiftN）。拆成两个方法必有一半答案是错的，OVERLAY 更是完全没覆盖。
- 改为单一 `getActiveBinding`，上下文判定与派发器同源。随之删掉 `getGuiBinding` / `getGameBinding`。
- 弃用 `GuideBindingResolver` 的 AUTO 探测：它拿"shift 绑定是否与 main 相同"当判据，那是引导展示的语义，不是派发语义。
- 新增 `BindingLayer` 常量类，避免消费方拼字符串。

**第二轮自审（实现可行性）**

- 上下文层改为按 `resolveTargetPhase()` 的优先级现算（Screen → gui / overlay 相位 → overlay / 否则 activeLayer），不读 `BindingMapper.phase`：该字段无 getter，且相位在 tick 里更新、比 Screen 打开晚一拍，会闪一帧错误的键。
- 补齐未绑定契约：`getLayer()` 永不为空、`getTriggerMode()` 回落 `PRESS`、`getActionId()` 把 null 归一为空串。
- `getTextFallback()` 补第三级回退（键盘标签 → 原始 buttonId），否则"每个按键都能画"的承诺在未知键上不成立。
- 补一条限制：API 不回答"这个动作属不属于当前界面"，按界面限定只能靠 per-Screen 覆盖。
