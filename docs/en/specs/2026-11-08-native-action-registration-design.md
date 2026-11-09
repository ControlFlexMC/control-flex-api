# Native action registration: bridges declare actions and default bindings (0.14.0)

> English translation of [`docs/zh/specs/2026-11-08-native-action-registration-design.md`](../../zh/specs/2026-11-08-native-action-registration-design.md).
> The Chinese file is the source; if they disagree, the Chinese one wins.
> Translated 2026-11-09.

**Status:** in review (shape settled, implementation not started)
**Target version:** `0.14.0`
**Date:** 2026-11-08
**Precondition:** 0.13.0 is untagged and unpublished; `IScreenHandler`, `IActionBindingProvider` and the built-in `hud_toggle` are all still local.
**Reference:** `cfx-compat-vehicles/docs/superpowers/specs/2026-11-08-native-action-registration-design.md` (the earlier game-layer native-action proposal).

## 1. Problem

### 1.1 Current state

The 18 battle actions in `cfx-compat-cobblemon` take the old road:

1. the bridge registers 18 pseudo `KeyMapping`s (namespace `cfx_compat_cobblemon`);
2. a compat JSON `screenKeys` section attributes them to `cobblemon`;
3. ControlFlex re-attributes the KeyMappings to `cobblemon:key.cfx_compat_cobblemon.battle_*`, and the bridge tolerates the rename through `candidateIds()`;
4. the GUI candidate list iterates the keys of `guiActionBindings`, so a per-screen override can only change "the button of an existing key".

### 1.2 Three costs of that road

- **Global exposure to Minecraft.** The 18 pseudo KeyMappings enter the vanilla controls screen and global key state, yet they only mean anything on `BattleGUI`.
- **Candidacy depends on a global table.** `hud_toggle` once "bound fine in settings, never dispatched at runtime" simply because the GUI candidate list did not contain that key. Every new action has to remember to add a row to the global table.
- **Defaults have nowhere to land.** GUI-layer default bindings are global and cannot express "this default only holds on `BattleGUI`".

### 1.3 The opportunity

None of these actions, APIs or core implementations have been published, so the whole thing can become "the bridge declares its actions directly to ControlFlex", bypassing Minecraft's KeyMapping registry and the global GUI layer.

## 2. Goals and non-goals

Goals:

- A bridge can register actions "only ControlFlex can see": stable id, display name, owning mod, layers, an optional Screen/Overlay class filter, and an optional default binding.
- Registered actions never enter Minecraft's `KeyMapping` registry and never write global key state.
- Screen/Overlay actions **do not enter** the global `guiActionBindings` / `overlayActionBindings` maps.
- ControlFlex calls the bridge when an action triggers; the bridge never polls key state.
- Default bindings apply when the user has not configured anything; a user's explicit clear is never re-seeded.
- In-game actions keep a level handle (`isActive()`) for "while held" semantics such as vehicles.

Non-goals:

- No analogue values (`getValue()` is reserved only).
- No runtime unregister / hot reload.
- No change to the existing contracts of `IScreenHandler` (back press + HUD state) or `IActionBindingProvider` (hint queries).
- No dedicated action or default binding for the built-in `hud_toggle`.
- No migration of built-in / legacy dynamic actions to the new storage (they keep their old path; see §6.4).

## 3. Concepts

### 3.1 A registration

One registration declares five pieces of information plus one default binding:

| Field | Example | Rule |
|---|---|---|
| `actionId` | `cfx_compat_cobblemon:battle_fight` | Must contain `:`; the prefix must be the plugin's `getModId()` |
| `nameKey` | `key.cfx_compat_cobblemon.battle_fight` | i18n key; falls back to actionId when empty |
| `groupModId` | `cobblemon` | UI grouping; falls back to the plugin mod id when that mod is absent |
| `layers` | `{SCREEN}` | Multi-select; declaring none means `IN_GAME + SCREEN + OVERLAY` |
| `classes` | `{...BattleGUI}` | Only meaningful for SCREEN / OVERLAY; undeclared = every class in that layer; exact match, no inheritance |
| `default` | `leftBumper|PRESS` | One binding; seeded into every declared layer / class; undeclared = leave all empty |

### 3.2 Three kinds of membership

| layer | Internal phase | Binding storage | Where the default lands | Dispatch timing |
|---|---|---|---|---|
| `IN_GAME` | GAME (main / shift1..4) | the profile's layer maps | seeded into each game layer when the key is missing | no Screen / Overlay in front |
| `SCREEN` | GUI | per-screen table (`user_screen_overrides.json`) | resolution fallback under that Screen class | that Screen class is foreground |
| `OVERLAY` | OVERLAY | per-screen table (`user_screen_overrides.json`) | resolution fallback under that Overlay class | that Overlay class is foreground |

Screen/Overlay actions are **not written** into `guiActionBindings` / `overlayActionBindings`; candidacy comes from "declared scope + whether a binding exists", never from the global table.

### 3.3 Trigger semantics

The callbacks describe **edges of a state change**, not a per-tick repetition:

| Mode | `onActionTriggered` | `onActionReleased` | `isActive()` |
|---|---|---|---|
| `PRESS` / `DELAY_PRESS` / `LONG_PRESS` | the one trigger | not called | true only on the triggering tick |
| `RELEASE` / `TAP` / `DOUBLE_TAP` | the moment the release check succeeds | not called | true only on the triggering tick |
| `DOUBLE_PRESS` | the moment the second press check succeeds | not called | true only on the triggering tick |
| `HOLD` / `DELAY_HOLD` / `LONG_HOLD` / `DOUBLE_HOLD` | the moment it becomes active | once when it stops being active | true while held |
| `TOGGLE` / `DOUBLE_TOGGLE` | every switch to on | every switch to off | true while latched |

- Even though a HOLD binding participates in combo dispatch every tick during the GUI phase, the callback fires once per activation edge (`hud_toggle` once flipped 20 times a second when bound to HOLD — that was exactly this constraint missing).
- For one-shot modes `isActive()` is implemented with a "last triggered tick" field: true on the triggering tick, automatically false on the next one, independent of the GUI activity set (which only tracks HOLD).
- Callbacks run on the client main thread inside the input tick, before that frame renders. Exceptions are isolated and logged without interrupting other actions.

## 4. API

```java
public enum ActionLayer { IN_GAME, SCREEN, OVERLAY }

public interface IActionRegistrar {
    /** A duplicate id returns the existing handle idempotently; an invalid argument logs a warning and returns a handle that never fires. */
    IControlFlexAction register(ActionRegistration registration);
}

public final class ActionRegistration {
    public static Builder builder(String actionId);

    public static final class Builder {
        public Builder nameKey(String nameKey);
        public Builder groupModId(String modId);
        public Builder layer(ActionLayer layer);          // may be called repeatedly
        public Builder targetClass(String className);     // SCREEN / OVERLAY only
        public Builder defaultBinding(DefaultBinding b);  // optional
        public ActionRegistration build();
    }
}

public final class DefaultBinding {
    /** button uses the ButtonName constants; "leftStickUp|HOLD" is never exposed to callers. */
    public static DefaultBinding of(String button, TriggerMode mode);
}

public interface IControlFlexAction {
    String getActionId();
    /** Level: see the isActive column in §3.3. Always false when unbound / cleared. Main thread, readable every tick. */
    boolean isActive();
}

public interface ActionTrigger {
    String actionId();
    TriggerMode mode();
    ActionLayer layer();
    /** The Screen / Overlay class name that matched; null for IN_GAME. */
    String targetClassName();
}

public interface IControlFlexPlugin {
    /** Called before onControlFlexReady and before the profile loads; the registrar is valid only inside the callback. */
    default void onRegisterActions(IActionRegistrar registrar) {}

    default void onActionTriggered(ActionTrigger trigger) {}

    /** Entering an inactive state (HOLD family / TOGGLE family); one-shot modes are not called. */
    default void onActionReleased(String actionId) {}
}
```

Validation:

| Condition | Behaviour |
|---|---|
| id empty / no `:` / prefix is not the plugin mod id | warning + a handle that never fires |
| duplicate id (same plugin) | return the existing handle idempotently + warning |
| duplicate id (across plugins) | first registrant wins; the later one gets a **handle that never fires** + warning (another plugin's handle is never handed out) |
| `targetClass` without SCREEN / OVERLAY | warning + the filter is ignored |
| SCREEN/OVERLAY declared but the `targetClass` does not exist yet | allowed (the class may load later); the binding sleeps |
| unknown default button | warning + that default is skipped |
| calling the registrar after the callback returns | throws `IllegalStateException` (a programming error, not silent) |

## 5. Lifecycle

`ControlFlexApiAdapter.discoverPlugins()` today:

```
onInstallCompatConfigs
onInstallGuideAssets
cameraLookConsumer
onControlFlexReady
```

becomes:

```
onInstallCompatConfigs
onInstallGuideAssets
cameraLookConsumer
onRegisterActions(registrar)   // new: the registrar is valid only here
onControlFlexReady
```

Registration must happen before `bindingRegistry.loadFromJson` for defaults to participate in resolution (the same timing requirement as vehicles §4.3). Bridges that do not implement the new callback behave as before (default method).

## 6. Storage and resolution

### 6.1 Screen / Overlay: reuse the existing per-screen table

No new file. A registered action's per-screen binding goes into **the table the per-screen editor already reads and writes**:

`config/controlflex/compat/user_screen_overrides.json` → `screens.<fqn>.<actionId>` (overlays go under `overlays`).

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

- The accessor stays `UserScreenOverrideStore` (`contains` / `get` / `put` / `removeAction` / `save`).
- Only **values the user changed** are stored, including an explicit `""` ("I want this empty"). Registration defaults exist only as an in-memory fallback, are never written on first run, and need no migration script.
- Resolution order: `contains` is true → use it (`""` = unbound); otherwise the registration default; otherwise unbound.
- Presence must be decided with `contains`, never inferred from "the parsed result is empty", or a clear would look unconfigured and the default would come back.
- Call `save()` after writing; `save()` also invalidates the foreground resolution cache, so the editor and the hints see the new value on the same frame.

**Why not a second store (the first draft did exactly that and it failed in game).** The per-screen editor (`EntryConfigScreen`) has always read and written this table. Adding `screen_action_bindings.json` made "saved successfully, shown correctly in the UI, unresolvable at runtime" all true at once — the resolver read only the new file and returned an empty binding before the legacy foreground override was ever consulted, so the editor's value never had a chance. One concept, one table, or there are two truths.

**Why not compat's `screen.<fqn>.bindings`.** That requires the mod to ship a compat file and maintain a `screenKeys` section; after the cobblemon migration the compat file keeps grouping metadata only and action attribution no longer goes through compat. `user_screen_overrides.json` is decoupled from compat files, which is exactly right.

### 6.2 Resolution entry: reuse `getEffectiveBinding`

Registered actions do not get a second resolution chain; they plug into the single existing choke point, `BindingRegistry.getEffectiveBinding(actionId, layer)`:

```
getEffectiveBinding(id, layer):
  if id is not registered → existing logic (membership gate → per-screen table → layer maps)
  if id is registered:
    IN_GAME  → use the layer maps (defaults already seeded, see §6.3)
    SCREEN   → current Screen class in scope ? store/default : unbound
    OVERLAY  → current Overlay class in scope ? store/default : unbound
```

That way the following paths **do not need their own implementation**: combo construction, `isSuppressedBinding`, `DoubleHoldInterrupt`, the GUI/game event sinks, `IActionBindingProvider`, and hint-versus-dispatch consistency. The resolver itself is still a separate class (`RegisteredActionResolver`), merely called by `getEffectiveBinding`.

Supporting constraints:

- `BindingRegistry.getActionIds()` merges registered ids (invalidating `cachedActionIds`) so the full-id iteration in the game / Overlay phases sees them.
- `BindingSerializer`'s two layer load / export paths **explicitly skip** registered ids: they do not belong to `mainActionBindings` / `guiActionBindings` / `overlayActionBindings`, and handling them normally would recreate global map entries.
- `isActionRelevantForLayer` returns "the declared layer" for registered actions so the UI considers them editable; the serialization skip is guaranteed separately by the previous bullet, not by relevance.
- `ActionCandidacy`: GUI candidacy for a registered action = scope match + resolved as bound; the map membership check is gone.

Screen and Overlay class names come from `MCCompat.getScreen(mc)` and `ScreenHistory.currentOverlayClass()` respectively — the same source `IScreenHandler` uses to resolve its target.

### 6.5 Registry seams (must be matched one by one when implementing)

| Seam | Behaviour |
|---|---|
| `getActionIds()` | Merge registered ids; clear `cachedActionIds` on registration (the UI row cache uses the same identity token) |
| `isActionRelevantForLayer` | A registered action is relevant only to its declared layers: `IN_GAME → main/shift1..4`, `SCREEN → gui`, `OVERLAY → overlay`. **Must come before the "contains a colon = configurable on every layer" branch**, or screen actions are treated as global |
| `BindingSerializer` layer load / export | Explicitly skip registered ids so they never land in `guiActionBindings` / `overlayActionBindings` (the return of global exposure) |
| `shiftRelevantActionIds()` | Satisfied automatically by the previous row: a SCREEN action is irrelevant to `shift1`, so templates need no empty entry and `TemplateV2ConformanceTest` is unaffected |
| `getActionDisplayName` / `getActionLocalizedName` | Registered actions read `nameKey`, falling back to the actionId |
| UI grouping | New `getActionGroupModId(id)`: registered actions return `groupModId`, everything else falls back to the existing `dynamicActionModId` |
| `setActionBinding(id, binding, GUI/OVERLAY)` | Registered actions write the per-screen table (the same one the editor uses), never the layer maps (the only write entry point for UI key capture) |
| `resetActionBinding(id, GUI/OVERLAY)` | Registered actions delete the key from the per-screen table (falling back to the default); `""` is not written |
| Test isolation | `RegisteredActionRegistry` is a process-wide singleton with a package-visible `clearForTests()`; clearing after the registration window closes does not affect production |

### 6.3 IN_GAME: reuse the layer maps

Consistent with the vehicles proposal: when the key is missing, seed the default binding into every declared game layer; when the profile already has a value (including `""`), respect it. Defaults change memory only and are written naturally the next time the profile is saved.

In v1 `IN_GAME` expands to five layers, `main + shift1..4`, with the same default written into each (otherwise the action disappears while the player holds a shift layer). Per-layer defaults are out of scope (§17).

### 6.4 Relationship to built-in / legacy dynamic actions

All three share one per-screen table (§6.1); they differ only in **candidacy** and **where defaults come from**:

| Action kind | Per-screen binding source | Candidacy | Defaults |
|---|---|---|---|
| Registered | `user_screen_overrides.json` | declared layer + class | the registration's `default` |
| Built-in (including `hud_toggle`) | the same table + compat screen entries | layer map membership | `DefaultBindings` / templates |
| Legacy dynamic (compat JSON `screenKeys`) | the same table | layer map membership | compat config |

Because there is only one table, the editor never merges two sources and there is no "which one wins" question. Moving built-in actions onto the registration mechanism is follow-up work, out of scope here (§15).

## 7. Dispatch

Candidate construction:

| Phase | Candidate source |
|---|---|
| SCREEN | the existing `guiActionBindings` iteration **+** the registered actions whose scope matches the current Screen |
| OVERLAY | the existing full-id iteration (`getActionIds` already contains registered actions), filtered at resolution time by the current Overlay class |
| GAME | same; `IN_GAME` actions resolve through the layer maps, everything else resolves as unbound |

- Registered Screen/Overlay actions **skip** `isGUIAction`'s category check: scope is candidacy.
- Binding resolution goes through §6.2's `getEffectiveBinding`; unbound = not part of any combo, no callback.
- No extra `ActionBinding` is constructed: `getEffectiveBinding` already returns one, and combo construction, suppression and state tracking all reuse the existing paths.
- Callbacks hang off the **combo event sink** (`EventSink`), not the per-tick `handleGUIAction`: the latter runs once per tick for HOLD, and attaching there would bring back the "`hud_toggle` flips 20 times a second" bug. Which event is taken per mode:

| Mode | Event used for `onActionTriggered` | Event used for `onActionReleased` |
|---|---|---|
| `PRESS` / `DELAY_PRESS` / `LONG_PRESS` | `PRESS` / `DELAY` / `LONG_PRESS` | — |
| `RELEASE` / `TAP` / `DOUBLE_TAP` | `RELEASE` / `TAP` / `DOUBLE_TAP` | — |
| `DOUBLE_PRESS` | `DOUBLE_PRESS` | — |
| `HOLD` / `DELAY_HOLD` / `LONG_HOLD` / `DOUBLE_HOLD` | the first activation event (`PRESS` / `DELAY` / `LONG_PRESS`) | `RELEASE` or `DESTROY` |
| `TOGGLE` / `DOUBLE_TOGGLE` | the switch to on | the switch to off |

- The game layer and the GUI layer each have their own combo manager, but their events share a shape; the callback implementation attaches to the outlet both use, so behaviour cannot diverge between phases.
- When a Screen and an Overlay are both present, the existing priority holds: Screen first.
- One action receives one callback: even if it matches several bindings at once (e.g. a chord and a single button collide), the combo that actually activated wins.

`ActionCandidacy` is adjusted accordingly:

```
built-in / legacy dynamic: unchanged (layer map membership + category)
registered:               scope match + resolved as bound
```

Registered actions **bypass** `LayerMembershipStore`: they have no notion of "enabled per layer" and do not appear in the membership toggles.

## 8. Relationship to the existing API

| Capability | Change |
|---|---|
| `IScreenHandler` / Registry | Unchanged: back press and HUD state are still registered per screen class |
| `IActionBindingProvider` | Resolution branches by action kind; registered actions return the per-screen table / default, so hints match actual dispatch |
| Built-in `hud_toggle` | Unchanged; no cobblemon-specific toggle action, no GUIDE default |
| `IInteractiveContextRegistrar` | Unchanged (Overlay foreground registration; Overlay actions match the class name it reports) |
| `IActionStateProvider` | Unchanged; `isActive()` for new actions is maintained by the registry itself (HOLD uses the activity set, one-shot modes use the last-trigger tick) |

## 9. UI

- "Mod adaptation → `<groupModId>`" lists the actions that mod registered; Screen actions appear in the per-screen binding editor for their screen.
- Registered actions **do not appear** in the global GUI / Overlay action lists (they are not in those maps).
- The per-screen editor and the resolver read and write the same table (§6.1); there is no "UI shows A, runtime uses B" split.
- The editor must show provenance: `User` / `Default`. Two distinct operations:
  - **Restore default**: delete the key from the per-screen table (falling back to the registration default).
  - **Clear**: write `""` (permanently empty, no fallback).
- Conflict detection stays as it is: same scope, same button chord, same trigger mode counts as a conflict; a default colliding with something like `move_forward` sharing a stick is reported, not blocked (matching vehicles D4).

## 10. Core implementation map

Added / changed (names may follow the implementation):

| Location | Content |
|---|---|
| `api/ActionLayer` `ActionRegistration` `DefaultBinding` `IActionRegistrar` `IControlFlexAction` `ActionTrigger` | new types |
| `api/IControlFlexPlugin` | three default callbacks |
| `api/ControlFlexApi` | no new accessor (the registrar is only handed out through the plugin callback) |
| `compat/RegisteredActionRegistry` | definitions + handles + owning plugin; open / close the registration window; scope lookup by class |
| `compat/RegisteredActionResolver` | per-screen table / default resolution and writes (`storeBinding` / `clearBinding`); called by `getEffectiveBinding`, shared by dispatch / provider / UI |
| `compat/UserScreenOverrideStore` | the single per-screen binding store, shared by the editor and registered-action resolution |
| `binding/BindingRegistry` | `getEffectiveBinding` branching (§6.2); `getActionIds` merges registered ids and invalidates the cache; localised name / grouping queries |
| `binding/BindingSerializer` | both layer load / export paths explicitly skip registered ids |
| `binding/BindingMapper` | GUI candidates append the registered actions in scope for the current Screen; game / Overlay keep the full-id iteration |
| Combo event outlet (`EventSink` consumer, shared by game and GUI) | emits `onActionTriggered` / `onActionReleased` per §7's event map; **not** attached to the per-tick `handleGUIAction` |
| `gui/GuiActionHandler` | entry branch: registered action → plugin callback and return; everything else keeps the existing switch |
| `binding/ActionDispatcher` | entry branch: registered IN_GAME actions → plugin callback, no key injection |
| `binding/ActionCandidacy` | registered actions judged by scope |
| `compat/ActionBindingProviderImpl` | resolution dispatch (registered → resolver; everything else → current behaviour) |
| `compat/ControlFlexApiAdapter.discoverPlugins` | insert `onRegisterActions` |
| UI per-screen binding editor / mod adaptation panel | list registered actions, provenance, restore default |

## 11. Migrating cobblemon

| Item | Change |
|---|---|
| 18 actions | ids become `cfx_compat_cobblemon:battle_*`; `layers={SCREEN}`, `classes={BattleGUI}` |
| Default bindings | `battle_prev=leftBumper|PRESS`, `battle_next=rightBumper|PRESS`, `battle_log_up=rightStickUp|PRESS`, `battle_log_down=rightStickDown|PRESS`; the other 14 stay empty |
| Removed | pseudo `KeyMapping` registration, `BattleKeys`, the compat JSON `screenKeys` section, `candidateIds()`, `consumeClick` in the ticker |
| Kept | `BattleScreen` (reflective clicking), `BattleSelection`, `BattleHud`, `BattleScreenHandler`; the compat JSON keeps only grouping metadata such as `mod_id` / `mod_name` / `home_page` |
| Triggering | the callback dispatches by actionId into the existing logic (click a tile / move the highlight / scroll the log) |
| Old bindings | old ids (`cobblemon:key.cfx_compat_cobblemon.*`) are void and players rebind; unpublished, so acceptable |

Log scrolling uses PRESS by default (each press scrolls a fixed number of lines); if "hold to keep scrolling" is wanted later, the repeat-interval semantics must be defined first (§15).

## 12. vehicles compatibility

vehicles' four actions register with `layers={IN_GAME}` and seed their defaults into main + shift1..4; the bridge reads the level through `isActive()`. Same registration item as this design, just without `classes`.

## 13. Versioning and release order

1. `control-flex-api` 0.14.0 (new types + plugin callbacks).
2. `control-flex` core implementation, `cfx_api_version` raised to 0.14.0.
3. `cfx-compat-cobblemon` / `cfx-compat-vehicles` migrate separately.

0.13.0 is unpublished, so it evolves straight to 0.14.0 with no compatibility layer.

## 14. Tests

Core:

- Registration validation: namespace, duplicate ids, `targetClass` combined with layers, unknown buttons, registering after the window closes.
- Default resolution matrix: no user value → default; `""` → unbound; user value → user value; verified for SCREEN / OVERLAY / GAME membership separately.
- Store round-trip: `""` must survive as a key (still empty after a restart, no fallback to the default).
- Candidates: another Screen class does not dispatch; registered actions of the same class join the combo; unbound ones do not; the legacy path is unaffected.
- Callbacks: PRESS once; TAP on release; HOLD triggers and releases once each; TOGGLE once per flip; exceptions isolated.
- `isActive()`: true while held for HOLD, false on release; one-shot modes only on the triggering tick; always false when unbound.
- Hint query: `getActiveBinding` returns the store / default for registered actions, unbound when the context does not match.
- Provider / Candidacy / dispatch reach the same conclusion for the same action.

Bridge:

- All 18 actions trigger their tile through the callback; highlight / cursor / log behaviour unchanged.
- The default LB/RB and right-stick up/down work out of the box; restore-default and clear are distinguishable.

## 15. Failure modes and edges

| Scenario | Behaviour |
|---|---|
| A registration callback throws | that plugin is isolated, the others keep registering; the stack is logged |
| An action callback throws | logged, and dispatch of other actions continues |
| The store is corrupt | treated as empty + warning; the original file is not overwritten until the next user change |
| A plugin is unloaded / not present | its actions are not registered, store entries remain (sleeping); reinstalling restores the bindings |
| Two plugins claim the same id | first registrant wins, the later one warns |
| One action matches both a Screen and an Overlay | Screen wins (the existing phase priority) |
| The player cleared a binding, then reinstalled the mod | the store holds `""`, so it stays empty |
| A class is renamed / a screen class disappears | the binding stays under the old class name; the new class uses the default |

## 16. Decision record

| # | Decision | Rationale |
|---|---|---|
| D1 | Registered actions create no `KeyMapping` | The whole point is to stop exposing them to Minecraft globally |
| D2 | Screen/Overlay actions stay out of the global maps | Candidacy comes from scope, which removes "no key in the table = never dispatched" |
| D3 | One default binding applies to every declared layer / class | Neither consumer needs per-layer values today; add a map when one does |
| D4 | `hud_toggle` stays built-in: no GUIDE default, no dedicated action | It is a general capability and should not be copied per mod |
| D5 | Callbacks fire on trigger, with a separate release callback for HOLD / TOGGLE | The bridge does not poll and does not lose a press |
| D6 | Resolution priority: user store > registration default | A user's clear must be permanent |
| D7 | Class filtering is exact, no inheritance | Consistent with `IScreenHandler`; limits the blast radius |
| D8 | A class that is not loaded keeps its binding and sleeps | Consistent with vehicles' "register even when IV is absent" |
| D9 | Built-in and legacy dynamic actions keep their path | Migration can proceed bridge by bridge, no big-bang switch |
| D10 | Callbacks are edge-based; HOLD does not repeat per tick | Prevents the 20-times-a-second `hud_toggle` incident from recurring |
| D11 | Registered actions bypass the layer membership toggles | They have no "enabled per layer" semantics; avoids meaningless switches |
| D12 | The per-screen table stores user values only; defaults are never written | No migration script; restore default = delete the key, clear = write `""` |
| D13 | Registered actions plug into the single `getEffectiveBinding` choke point instead of each call site | Suppression, double-press, release and hint queries all reuse existing paths, so behaviour cannot fork |
| D14 | Relevance (visible in the UI) and serialization (written or not) are judged separately | Screen actions must look editable to the UI yet never enter the global maps |
| D15 | Registered actions reuse `user_screen_overrides.json` instead of a second store | The per-screen editor is the existing write entry point; a second store made "saved successfully, hints missing" true at once (observed in game) |

## 17. Open questions

1. Should per-layer default bindings (`Map<ActionLayer, DefaultBinding>`) be part of 0.14.0? Current answer: no.
2. Should registered Screen actions also appear in the "main binding list" (outside the per-screen editor)? Current answer: no — that would create a second truth.
3. Should built-in actions' per-screen bindings also move to the registered-action store (single storage, no compat dependency)? Current answer: out of scope. *(Translator's note: moot after D15 — there is only one table now.)*
4. If log scrolling switches to HOLD, the repeat-interval semantics must be defined first; out of scope.
