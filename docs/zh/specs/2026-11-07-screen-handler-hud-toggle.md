# 每屏幕处理器与 HUD 开关（0.9.2.1）

> 英文版：[English](../../en/specs/2026-11-07-screen-handler-hud-toggle.md)
**状态：** 待确认（形状已定，实现未开始）
**目标版本：** `0.9.2.1`
**日期：** 2026-11-07
**前置：** 早先的 `IGuiBackHandler` 只发过 mavenLocal、没打 tag、没有外部消费者，所以直接演进不做兼容层。

## 1. 目标与非目标

目标：

- 一个屏幕类只对应一个处理器对象，同时提供「返回」和「HUD 开关」两个钩子。
- HUD 开关是 ControlFlex 的**内置动作**，任何界面都能绑一次即可；桥接模组不再各自导出 toggle 动作。
- 开关状态由 ControlFlex 持久化，按屏幕类记忆；下次进入同一界面自动生效。

非目标：

- ControlFlex 自己不绘制任何 HUD（v1 的默认分支就是 no-op）。
- 继承匹配、通配符匹配、按包名批量注册。

## 2. 接口

```java
/** 这次可见性变化来自哪里。 */
public enum HudStateReason {
    /** 该类第一次出现，没有存档，使用默认值（显示）。 */
    DEFAULT,
    /** 从持久化里恢复了上次的值。 */
    RESTORED,
    /** 玩家按了 hud_toggle。 */
    USER_TOGGLE
}

public interface IScreenHandler {

    /** 吃掉返回键；false = 交给 ControlFlex 发 ESC。 */
    default boolean onBack() { return false; }

    /**
     * 声明本界面存在由本处理器绘制的提示，ControlFlex 才会为它翻转并记忆状态。
     * false = 这个类不接管 HUD，按下 hud_toggle 走默认（no-op），也不会写存档。
     */
    default boolean ownsHud() { return false; }

    /**
     * ControlFlex 通知最终状态；实现方只按它绘制，不负责翻转与记忆。
     *
     * <p>三个时机调用：类成为前台（Screen 打开 / overlay 进入前台）、处理器注册时该类
     * 已是前台、以及开关落盘之前。实现方可以假定第一次绘制之前已经收到过通知。</p>
     *
     * @param visible 该屏幕类的提示是否应该显示
     * @param reason  本次变化来自哪里
     * @return {@code true} = 已按通知应用；{@code false} = 应用失败——ControlFlex 保留
     *         原状态（toggle 不落盘），保证存档永远与屏幕实际显示一致，并记录警告
     */
    default boolean onHudState(boolean visible, HudStateReason reason) { return false; }
}

public interface IScreenHandlerRegistry {

    boolean register(String screenClassName, IScreenHandler handler);
    boolean unregister(String screenClassName);
    @Nullable IScreenHandler registeredFor(String screenClassName);

    /**
     * 该屏幕类的提示是否可见；从未切换过 = true。诊断与「一次画多个类」的查询用，
     * 不放进逐帧渲染路径——状态通过 {@link IScreenHandler#onHudState} 推送。
     */
    boolean isHudVisible(String screenClassName);
}
```

**为什么只有一个通知方法。** 上一版是 `onToggleHud`（决定新状态）加 `onHudStateRestored`（落盘后回推），两者都在说"现在该不该显示"，且状态会在两处被写入。统一后**翻转与记忆只属于 ControlFlex**：桥接收到 `onHudState` 就画，收到 `null`——不存在了，因为决定权不在桥接手上。这同时消除了"按自己的返回值再回推一次"这种绕路的流程。

**为什么需要 `ownsHud()`。** 注册一个 handler 只为 `onBack` 的模组不该被顺带当成 HUD 的主人——否则它每次按 toggle，ControlFlex 都会翻转并落盘一个没人绘制的状态，配置文件里积累垃圾条目，玩家看到的是按键毫无反应。显式声明比"注册即拥有"干净。

**Reason 的用途。** v1 里三个值对 Cobblemon 桥接没有行为差别，但它是这份契约里"为什么变"的唯一记录：将来 ControlFlex 自己的 HUD 要区分"恢复"（安静地恢复）与"用户按键"（给动作条反馈）时不用再改签名。给枚举加值是向后兼容的，桥接的 `switch` 必须带 `default`。

**为什么 `isHudVisible` 挂在注册表上。** 注册表和状态是同一张「按完整类名索引的表」。另开一个 provider 会让调用方为了「这一帧画不画」多拿一个访问器，而它本来就已经拿着注册表了。

## 3. 状态与持久化

- 文件：`config/controlflex/screen_hud_state.json`，形如 `{"com.cobblemon.mod.common.client.gui.battle.BattleGUI": false}`。
- 只写非默认值（`true` 不落盘），文件因此只有几条。
- 读：客户端初始化时加载一次到内存；查不到、解析失败、类名不匹配 → 一律 `true`。宁可多显示提示，也不要出现"按了没反应"。
- 写：值真正变化时才落盘。文件很小，不防抖。

### 3.1 状态怎么到达绘制方：推送，不是轮询

ControlFlex 在三个时机调用 `onHudState(visible, reason)`：

| 时机 | 为什么 |
| --- | --- |
| 该类成为前台（Screen 打开 / overlay 进入前台） | 让下一次绘制就用对的值；reason = 有存档 ? RESTORED : DEFAULT |
| 处理器注册时，若该类已经是前台 | 插件发现晚于界面打开时补一次，否则第一次会是默认值；reason 同上 |
| 玩家按下 hud_toggle 且 handler.ownsHud()，且 handler 报告应用成功 | reason = USER_TOGGLE |

实现方只存一个普通字段，渲染路径里读字段。**不要每帧 `isHudVisible()`**：一次 map 查表虽然只有十几纳秒，但那等于把"状态同步"降级成轮询，调用方还得自己判断"什么时候该重读"，而这个判断正是接口应该替他做掉的部分。

**首帧不闪的保证来自推送时机**，不是来自无脑每帧读：界面成为前台的推送发生在 ControlFlex 的 tick 里，早于该帧的渲染；注册时的补推覆盖了"插件比你晚一步"的情况。真要在绘制方自己兜底，正确的做法也不是每帧查表，而是缓存"上次看到的屏幕类"，只在类变化时查一次——身份比较而不是字符串哈希。

- 类被卸载后条目保留：无害，且插件可能在这之后才注册。

## 4. 内置动作与相位

- `ActionId.HUD_TOGGLE = "hud_toggle"`，分类 `GUI_ACTIONS`（显示在 GUI 分组里）。
- 允许在 **GUI / OVERLAY / GAME** 三个相位触发。ControlFlex 自有动作按 category 决定层归属，这里靠新增谓词 `ActionId.isCrossPhaseUiAction(id)`：
  - `BindingRegistry.isActionRelevantForLayer` 里提前 `return true`（与滚轮动作 `isMouseWheelAction` 同一模式）；
  - `BindingMapper.isGUIAction` 里也要放行，否则 GUI 候选会被 category 过滤掉；
  - overlay 层白名单 `OVERLAY_DEFAULT_ACTIONS` 不再需要为它单独加。
- 派发路径与 `GUI_BACK` 完全同构，两处各一个分支：
  - GUI 相位 → `GuiActionHandler.handleGUIAction` 新增 `case ActionId.HUD_TOGGLE`；
  - OVERLAY / GAME → `ActionDispatcher.activateAction` 的特判分支（`SYS_PAUSE` / `GUI_BACK` 就在那里）。

## 5. 目标界面的解析

1. `MCCompat.getScreen(mc) != null` → 用 `screen.getClass().getName()`；
2. 否则 `InteractiveContextRegistry.peekForeground()`（**需要新增只读方法**；现有 `popForeground` 会移除上下文，不能用）；
3. 都没有 → 无目标，走默认。

Screen 优先于 overlay：两者同时存在时，screen 盖在 overlay 上。

**in-game 的空白。** GAME 相位通常既无 Screen 也无 overlay 上下文，于是第 3 步落到默认、no-op。这是可接受的——目前没有人的 HUD 只存在于游戏内。将来若有，需要一个伪键（例如 `controlflex:in_game`）供其注册，否则没有任何类名可匹配。这条写在这里，避免以后误以为 bug。

## 6. 开关流程

```
按下 hud_toggle
 → 解析目标类
 → registry.registeredFor(cls)
     ├ null                    → 默认：no-op（v1）
     └ !handler.ownsHud()      → 默认：no-op，不写存档
     └ 新值 = !isHudVisible(cls)
        → handler.onHudState(新值, USER_TOGGLE)
              ├ false → 保留原状态，警告日志，不落盘
              └ true  → 存盘
```

翻转与记忆只有一处（ControlFlex），但**应用的成败由实现方回报**：通知发生在落盘之前，应用失败就不落盘——存档里的值永远等于屏幕实际显示的值。前台推送同理，失败只影响那一屏的显示，存档不动。

日志每次切换一行：`[ScreenHandler] hud toggle BattleGUI -> hidden (persisted)`。没有这行就说明没找到目标类或没有 handler，排查"为什么进界面提示不出现"只需看这一行。

## 7. cfx-compat-cobblemon 的迁移

- 删除 `battle_hud_toggle`（19 → 18），compat JSON 与 `zh_cn`/`en_us` 各删一条；「枚举与 JSON 一一对应」的单测会立刻发现漏删。
- `BattleHud.visible` 从"自己 toggle 的静态字段"改成"由 `onHudState` 写入的字段"；`toggle()` 删除——桥接不再有翻转逻辑。
- `BattleBackHandler` 改造为实现 `IScreenHandler`：`onBack` 保持现状，`ownsHud()` 返回 true，`onHudState` 写 `BattleHud` 的字段；注册键仍是 `BattleScreen.GUI_CLASS`。
- 玩家原来绑在 `battle_hud_toggle` 上的键会变成孤立绑定，需要重绑到新的内置 `hud_toggle`。这是删动作的代价，一次性。

## 8. 验收

- 单测：注册表一类一个（第二个 `register` 返回 false 且不覆盖）；`isHudVisible` 未登记返回 true；状态 round-trip。
- 单测：`ownsHud()=false` 的处理器收到 toggle 不翻转、不落盘；`ownsHud()=true` 翻转并收到 `USER_TOGGLE` 通知。
- 单测：`isCrossPhaseUiAction` 让 `hud_toggle` 在 GUI / overlay / main / shift 各层都被判为可用。
- 手测：绑定后战斗中按一次 → 提示消失 → 退出再进 → 仍是隐藏（不闪）→ 再按恢复 → 重启游戏仍保持。
- 手测：未注册处理器的界面（如原版背包）按 `hud_toggle` 无任何反应，也不报错。

## 9. 修订记录

- 2026-11-07 首次成文。三项决策：合并成 `IScreenHandler` 双钩子（`0.9.2.1`）；默认分支 v1 为 no-op；三个相位都允许。新增：状态持久化与「下次进入自动生效」的读取时机约束。
- 2026-11-07 第二轮：**状态改为推送（`onHudStateRestored`），删除「桥接每帧读」的要求**。原写法在性能上其实无害（一次 map 查表），但把状态同步降级成轮询，把"什么时候该重读"的判断推给了每个实现方；推送时机（前台变化 / 注册 / 落盘后）才是首帧不闪的真正保证。`isHudVisible` 保留但降级为诊断用途。
- 2026-11-07 第三轮：**通知统一为一个方法**。`onToggleHud(Boolean)`（决定）与 `onHudStateRestored`（应用）合并为 `onHudState(boolean, HudStateReason)`，翻转与记忆只属于 ControlFlex；新增 `ownsHud()` 声明接管，避免只为 onBack 注册的处理器被顺带当成 HUD 主人而产生垃圾存档。
- 2026-11-07 第四轮：`onHudState` 保留 **boolean 返回值**，语义改为「应用是否成功」。通知先于落盘：应用失败则不持久化、保留原状态，存档与屏幕显示永不分歧；前台推送失败只记警告。
