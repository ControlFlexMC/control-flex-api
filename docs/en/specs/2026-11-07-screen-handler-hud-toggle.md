# Per-screen handlers and the HUD toggle (0.9.2.1)

> English translation of [`docs/zh/specs/2026-11-07-screen-handler-hud-toggle.md`](../../zh/specs/2026-11-07-screen-handler-hud-toggle.md).
> The Chinese file is the source; if they disagree, the Chinese one wins.
> Translated 2026-11-09.

**Status:** agreed (shape settled, implementation not started)
**Target version:** `0.9.2.1`
**Date:** 2026-11-07
**Precondition:** the earlier `IGuiBackHandler` only ever reached mavenLocal — no tag, no external consumers — so it is evolved in place with no compatibility layer.

## 1. Goals and non-goals

Goals:

- One handler object per screen class, exposing both the back press and the HUD toggle.
- The HUD toggle is a ControlFlex **built-in action**: bind it once and it works on any screen; bridges no longer export their own toggle action.
- The state is persisted by ControlFlex, remembered per screen class, and applied automatically the next time that screen opens.

Non-goals:

- ControlFlex drawing any HUD itself (the v1 default branch is a no-op).
- Inheritance matching, wildcard matching, or bulk registration by package.

## 2. Interfaces

```java
/** Where this visibility change came from. */
public enum HudStateReason {
    /** First sighting of this class, nothing stored: the default (visible) applies. */
    DEFAULT,
    /** Restored from persistence. */
    RESTORED,
    /** The player pressed hud_toggle. */
    USER_TOGGLE
}

public interface IScreenHandler {

    /** Consume the back press; false = let ControlFlex send Escape. */
    default boolean onBack() { return false; }

    /**
     * Declares that this screen has hints drawn by this handler, so ControlFlex will flip and
     * remember state for it. false = this class does not own a HUD: hud_toggle stays a no-op and
     * nothing is persisted.
     */
    default boolean ownsHud() { return false; }

    /**
     * ControlFlex reports the final state; implementations draw from it and never flip or
     * persist anything themselves.
     *
     * <p>Called at three moments: the class becomes foreground (Screen opens / overlay enters
     * the foreground), the handler is registered while that class is already foreground, and
     * right before a toggle is written to disk. Implementations may assume the notification
     * arrives before the first frame that depends on it.</p>
     *
     * @param visible whether this screen class should show its hints
     * @param reason  what caused this change
     * @return {@code true} = applied as notified; {@code false} = could not apply — ControlFlex
     *         keeps the previous state (the toggle is not persisted), so the stored value always
     *         matches what the screen actually shows, and logs a warning
     */
    default boolean onHudState(boolean visible, HudStateReason reason) { return false; }
}

public interface IScreenHandlerRegistry {

    boolean register(String screenClassName, IScreenHandler handler);
    boolean unregister(String screenClassName);
    @Nullable IScreenHandler registeredFor(String screenClassName);

    /**
     * Whether hints are visible for this screen class; never toggled = true. For diagnostics and
     * "draw several classes at once" queries, not for the per-frame render path — the state is
     * pushed through {@link IScreenHandler#onHudState}.
     */
    boolean isHudVisible(String screenClassName);
}
```

**Why a single notification method.** The previous draft had `onToggleHud` (decide the new state) plus `onHudStateRestored` (push it back after persisting). Both said "should this be shown now", and the state was written in two places. After the merge, **flipping and remembering belong only to ControlFlex**: the bridge draws whatever `onHudState` delivers. The `null` case is gone too, because the decision is no longer the bridge's — which also removes the roundabout "call back with my own return value" flow.

**Why `ownsHud()` exists.** A mod that registers a handler purely for `onBack` should not be treated as a HUD owner: otherwise every toggle flips and persists a state nobody draws, the config file accumulates junk entries, and the player sees a button that does nothing. An explicit declaration is cleaner than "registration implies ownership".

**What `reason` is for.** In v1 the three values make no behavioural difference to the Cobblemon bridge, but this is the only record of *why* a state arrived: when ControlFlex draws its own HUD and needs to distinguish "restore" (stay quiet) from "user pressed" (show action-bar feedback), the signature will not have to change. Adding enum values is backward compatible; bridges must keep a `default` branch in their `switch`.

**Why `isHudVisible` lives on the registry.** The registry and the state are the same "table indexed by fully-qualified class name". A separate provider would force callers to hold one more accessor just to answer "draw this frame or not", when they already hold the registry.

## 3. State and persistence

- File: `config/controlflex/screen_hud_state.json`, e.g. `{"com.cobblemon.mod.common.client.gui.battle.BattleGUI": false}`.
- Only non-default values are written (`true` is never stored), so the file stays tiny.
- Read: loaded once into memory during client init; a missing entry, a parse failure or a class-name mismatch all resolve to `true`. Better to show hints than to produce "the key does nothing".
- Write: only when the value actually changes. The file is small enough that no debounce is needed.

### 3.1 How the state reaches the renderer: push, not poll

ControlFlex calls `onHudState(visible, reason)` at three moments:

| Moment | Why |
| --- | --- |
| The class becomes foreground (Screen opens / overlay enters the foreground) | So the next frame draws with the right value; reason = stored ? RESTORED : DEFAULT |
| Handler registration while that class is already foreground | Covers "the plugin was discovered after the screen opened"; otherwise the first frames use the default; same reason rule |
| The player presses hud_toggle, `handler.ownsHud()` is true, and the handler reports success | reason = USER_TOGGLE |

Implementations store one plain field and read it in the render path. **Do not call `isHudVisible()` every frame**: a map lookup costs tens of nanoseconds, but it downgrades state synchronisation to polling and leaves "when should I re-read" to every implementer — exactly the judgement the interface is supposed to make for them.

**A stable first frame comes from the push timing**, not from reading every frame: the foreground push happens in ControlFlex's tick, before that frame renders, and the registration push covers "the plugin is a step behind". If a renderer really wants its own fallback, the right shape is still not a per-frame lookup but caching "the last screen class I saw" and re-reading only when it changes — an identity comparison, not a string hash.

- Entries survive a class being unloaded: harmless, and the plugin may only register afterwards.

## 4. Built-in action and phases

- `ActionId.HUD_TOGGLE = "hud_toggle"`, category `GUI_ACTIONS` (so it shows in the GUI group).
- Triggerable in **GUI / OVERLAY / GAME**. ControlFlex's own actions derive their layer from their category, so this one uses a new predicate, `ActionId.isCrossPhaseUiAction(id)`:
  - return `true` early in `BindingRegistry.isActionRelevantForLayer` (the same pattern as the mouse-wheel `isMouseWheelAction`);
  - also allow it in `BindingMapper.isGUIAction`, otherwise the GUI candidate list filters it out by category;
  - the overlay whitelist `OVERLAY_DEFAULT_ACTIONS` no longer needs a separate entry.
- Dispatch mirrors `GUI_BACK` exactly, one branch in each place:
  - GUI phase → a new `case ActionId.HUD_TOGGLE` in `GuiActionHandler.handleGUIAction`;
  - OVERLAY / GAME → the special-case branch in `ActionDispatcher.activateAction` (where `SYS_PAUSE` / `GUI_BACK` already live).

## 5. Resolving the target screen

1. `MCCompat.getScreen(mc) != null` → use `screen.getClass().getName()`;
2. otherwise `InteractiveContextRegistry.peekForeground()` (**a new read-only method**; the existing `popForeground` removes the context and cannot be used);
3. neither → no target, default behaviour.

Screen wins over overlay: when both exist, the screen covers the overlay.

**The in-game gap.** The GAME phase usually has neither a Screen nor an overlay context, so step 3 lands on the default, a no-op. That is acceptable — nobody's HUD exists only in-game today. If one ever does, it needs a pseudo key (e.g. `controlflex:in_game`) to register against, because there is no class name to match. Recorded here so it is not mistaken for a bug later.

## 6. Toggle flow

```
hud_toggle pressed
 → resolve the target class
 → registry.registeredFor(cls)
     ├ null                    → default: no-op (v1)
     └ !handler.ownsHud()      → default: no-op, nothing persisted
     └ newValue = !isHudVisible(cls)
        → handler.onHudState(newValue, USER_TOGGLE)
              ├ false → keep the previous state, warn, do not persist
              └ true  → persist
```

Flipping and remembering live in one place (ControlFlex), but **success is reported by the implementer**: the notification happens before the write, so a failed apply is never persisted — the stored value always equals what the screen actually displays. Foreground pushes work the same way; a failure only affects that screen's rendering, the stored value is untouched.

One log line per toggle: `[ScreenHandler] hud toggle BattleGUI -> hidden (persisted)`. No line means no target class or no handler; "why do the hints not appear in this screen" is answered by that single line.

## 7. Migrating cfx-compat-cobblemon

- Delete `battle_hud_toggle` (19 → 18) and one entry from the compat JSON, `zh_cn` and `en_us`; the "enum matches JSON one-to-one" unit test catches any missed deletion immediately.
- `BattleHud.visible` changes from "a static field the mod toggles" to "a field written by `onHudState`"; `toggle()` is deleted — the bridge holds no flipping logic any more.
- `BattleBackHandler` becomes an `IScreenHandler`: `onBack` unchanged, `ownsHud()` returns true, `onHudState` writes the `BattleHud` field; the registration key is still `BattleScreen.GUI_CLASS`.
- Keys players had bound to `battle_hud_toggle` become orphaned and must be rebound to the built-in `hud_toggle`. That is the one-time cost of deleting an action.

## 8. Acceptance

- Unit: one handler per class (a second `register` returns false and does not overwrite); `isHudVisible` returns true for an unregistered class; state round-trips.
- Unit: a handler with `ownsHud()=false` does not flip or persist on toggle; `ownsHud()=true` flips and receives a `USER_TOGGLE` notification.
- Unit: `isCrossPhaseUiAction` makes `hud_toggle` relevant on the GUI / overlay / main / shift layers.
- Manual: after binding, press once in battle → hints disappear → leave and re-enter → still hidden (no flash) → press again to restore → restart the game and it is still remembered.
- Manual: pressing `hud_toggle` on a screen with no registered handler (e.g. the vanilla inventory) does nothing and logs nothing.

## 9. Revision history

- 2026-11-07, first version. Three decisions: merge into `IScreenHandler` with two hooks (`0.9.2.1`); the v1 default branch is a no-op; all three phases are allowed. Added: state persistence and the read-timing constraint for "applies automatically next time".
- 2026-11-07, second round: **state becomes push (`onHudStateRestored`), the "bridge reads every frame" requirement is removed**. The original wording was harmless performance-wise (one map lookup) but downgraded synchronisation to polling and pushed "when to re-read" onto every implementer; the push moments (foreground change / registration / after persisting) are the real guarantee of a stable first frame. `isHudVisible` stays, demoted to diagnostics.
- 2026-11-07, third round: **the notifications collapse into one method**. `onToggleHud(Boolean)` (decide) and `onHudStateRestored` (apply) merge into `onHudState(boolean, HudStateReason)`; flipping and remembering belong only to ControlFlex. `ownsHud()` was added so a handler registered only for `onBack` is not mistaken for a HUD owner and does not accumulate junk state.
- 2026-11-07, fourth round: `onHudState` keeps its **boolean return**, now meaning "applied successfully". The notification precedes the write: a failed apply is not persisted and the previous state is kept, so the file and the screen never disagree; a failed foreground push only logs a warning.
