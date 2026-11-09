# Action Binding Query API design

> English translation of [`docs/zh/specs/2026-11-06-action-binding-query-design.md`](../../zh/specs/2026-11-06-action-binding-query-design.md).
> The Chinese file is the source; if they disagree, the Chinese one wins.
> Translated 2026-11-09.

**Status:** interfaces landed (revised after two self-reviews); implementation in the main repo pending
**Branch:** to be created (suggested `feat/action-binding-query`, off `main`)
**Target version:** `0.9.2.1` (written as `0.10.0` at the time; the numbering has since been normalised)
**Date:** 2026-11-06

---

## 1. Background

A bridge mod needs to draw its own "which button fires this action right now" hints on screen. Typical case: the Cobblemon battle screen maps four option tiles to four actions, and the bridge wants to label each tile with the controller glyph for the player's current binding.

The API cannot do this today; two pieces of data are missing.

1. **Which button an action is bound to.** `IActionStateProvider.isGameActionActive()` only answers "is this firing right now"; `IInputProvider.getControllerState()` only exposes raw hardware state. There is no `actionId` → bound button query.
2. **Which glyph to draw.** The glyphs are not in the API. They live in a private implementation class in the main repo (`ControllerGlyphRenderer`) and are an **atlas**, not individual files: `controlflex:textures/gui/controller/controller_{xbox|playstation|switch}.png`, 512×128, 16 columns, 32×32 per cell, where the cell index comes from `controllers/glyph_mappings/<skin>.json`.

A bridge that maintains its own glyph set will disagree with the skin the player actually uses (Xbox / PS / Switch, including the DualSense check); a bridge that guesses buttons will be wrong the moment the player rebinds. Both must come from ControlFlex.

## 2. Goals / non-goals

**Goals**

- One call returns "what this action is bound to and what to draw", self-consistently (never a button from one skin and a glyph from another).
- Consumers do not need to know about atlas layout, skin selection, shift chording or keyboard fallbacks.
- Read-only, side-effect free, safe to call every frame during rendering.

**Non-goals**

- No "change a binding from code" (writes belong to the config UI).
- No "query the binding table of an arbitrary Screen" (only the currently effective context; see 3.3).
- No drawing helpers (the API has no Minecraft dependency; see 3.1).

**Anti-goals**

- Letting consumers cache the result. Bindings change on rebind, controller swap and screen open/close; a cache is guaranteed to go stale.
- Putting "is this action currently suppressed / can it fire" into the binding info. That is the read side of `IPlayerStateRegistry`, a different concern.

## 3. Constraints

### 3.1 No Minecraft dependency

`control-flex-api` is a standalone `java-library` built with `--release 8` and does not depend on Minecraft. Therefore:

- Types such as `GuiGraphics`, `ResourceLocation` or `Component` cannot appear.
- Textures can only be expressed as a string id (`"controlflex:textures/gui/controller/controller_xbox.png"`).
- **A convenience method such as `drawAt(GuiGraphics, ...)` is impossible** — the most easily overlooked consequence of this section.

### 3.2 Threading and lifecycle

Client main thread, during rendering only. The return value is a snapshot of that resolution and must not be reused across frames.

### 3.3 Context is "currently effective"

The main repo's `BindingRegistry.getEffectiveBinding(actionId, layer)` already goes through `ForegroundBindingResolver`, which layers the foreground Screen's per-FQN overrides (`user_screen_overrides.json`) on top. The API therefore needs neither a `Screen` parameter nor an FQN from the caller.

The one thing consumers must accept: the answer varies with the current phase (GAME / OVERLAY / GUI) and the current shift layer, and cannot be known before the screen opens. That is deliberate — see 5.2; better to answer only "right now" than to offer a second semantics that can diverge from dispatch.

### 3.4 Backward compatibility

`package-info` already promises "new methods are added as `default`". This design **changes no existing interface**, only adds interfaces and enums, so it is a minor release.

## 4. Design decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Number of interfaces | **One** `IActionBindingProvider` returning an aggregate object | Buttons and glyphs are two projections of the same resolution; two separate queries can tear if the skin or binding changes in between |
| Texture representation | **A rectangle**: `textureId + u/v/w/h + texWidth/texHeight` | Does not expose the notion of an atlas. If it ever becomes one small PNG per glyph, set `u/v` to 0 and `w/h` to the glyph size; the API stays the same |
| Unbound | Return a **non-null** info object with `isBound()` | Saves the caller a null check; consistent with the existing "provider may be null, result never is" habit |
| Missing glyph | `getTextFallback()` returns drawable text (`"F1"`, `"HOME"`) | Keyboard keys and unknown hardware have no atlas cell but must still be drawable |
| Layer | **A single** `getActiveBinding` that uses the same context rules as dispatch | The first draft's "one method for GUI, one for game" premise was wrong: there are three phases (`InputPhase` = GAME / OVERLAY / GUI), and GAME uses `getActiveLayer()`, which may be shiftN. Splitting into two methods guarantees half the answers are wrong — see 5.2 |
| Shift chords | **Flattened** into `getButtons()` / `getGlyphs()`, modifier first | Matches `ResolvedGuideBinding.glyphButtonIds()`; a structured `getModifier()` invites "name includes the modifier, glyphs do not" |
| Layer constants | New `BindingLayer` string-constant class | Same style as `ButtonName`; stops consumers hand-building `"shift1"` |
| Trigger mode | A mirrored `TriggerMode` enum | Consistent with `InputMode` and `ControllerType`; mapped by name, unknown values fall back to `PRESS` |
| Ship per-glyph PNGs too | **No** | The rectangle already hides atlas details; a second resource set means a second generator and a second skin sync |
| Expose `isFromScreenOverride()` | **No** | The HUD does not need it; the case that does is debugging the config UI, which deserves its own query |

## 5. API shape

### 5.1 Entry point

```java
// ControlFlexApi
public static IActionBindingProvider getActionBindingProvider();   // null when not installed
```

### 5.2 Query

```java
public interface IActionBindingProvider {

    /**
     * Resolve actionId in the currently effective input context.
     * Context determination matches the dispatcher; the result is never null.
     */
    IActionBindingInfo getActiveBinding(String actionId);
}
```

**Why only one method.** The first draft had `getGuiBinding` / `getGameBinding`; the self-review found the premise itself wrong. ControlFlex input is not "two layers" but three phases.

```java
enum InputPhase { GAME, OVERLAY, GUI }          // binding/InputPhase.java

// binding/PhaseTransition.java:23
BindingContext.of(phase, activeLayer) →
    GAME    → activeLayer (main or shift1..4)
    OVERLAY → LAYER_OVERLAY
    GUI     → LAYER_GUI
```

The phase is decided by `BindingMapper.resolveTargetPhase()`, in the order **Screen open → GUI**, **foreground interactive overlay → OVERLAY**, otherwise GAME. So:

- a bridge that implements the battle screen as an ordinary Screen gets `gui`;
- one that registers it as an interactive overlay through `IInteractiveContextRegistrar` gets `overlay` — which the original design deferred to "v2" yet is the most natural fit;
- with the battle minimised to a HUD, the answer depends on the active shift layer, and `getGameBinding(main + auto-shift)` would report the wrong one.

One method covers all three cases and removes any "hint value vs dispatched value" split.

### 5.3 Result

```java
public interface IActionBindingInfo {

    String getActionId();

    boolean isBound();

    /**
     * Bound button ids in chord order; empty when unbound.
     * On a shift layer the modifier comes first, e.g. {@code ["leftBumper", "buttonX"]}.
     */
    List<String> getButtons();

    /** "A" / "LB + X" / "F1", localised to the controller skin. Empty when unbound. */
    String getDisplayName();

    /** Same length and order as getButtons(); every entry is drawable (glyph or text). */
    List<IControllerGlyph> getGlyphs();

    /** The layer consulted for this query (see BindingLayer); reported even when unbound. */
    String getLayer();

    TriggerMode getTriggerMode();
}
```

Unbound contract (no exception, no null): `getButtons()` / `getGlyphs()` are empty lists, `getDisplayName()` is an empty string, `getLayer()` still reports the consulted layer (never empty), `getTriggerMode()` is `PRESS`, and `getActionId()` normalises `null` to an empty string. Passing `null` or an empty `actionId` also returns an unbound info.

**Shift chords are flat; the modifier is not returned separately.** This matches the main repo: `ResolvedGuideBinding.glyphButtonIds()` puts the modifier before the action button in one list. There is no `getModifier()`, because "`getDisplayName()` includes it, `getGlyphs()` does not" satisfies only one of the two, and consumers will almost certainly draw the modifier twice or not at all. To tell whether this is a shift chord, read `getLayer()`.

### 5.4 Glyph

```java
public interface IControllerGlyph {

    /** Texture resource id, e.g. "controlflex:textures/gui/controller/controller_xbox.png". */
    String getTextureId();

    int getU();
    int getV();
    int getWidth();
    int getHeight();

    int getTextureWidth();
    int getTextureHeight();

    /** Non-null when there is no atlas cell (keyboard key, unknown pad); draw it instead. */
    @Nullable String getTextFallback();
}
```

### 5.5 Drawing (consumer side)

```java
IControllerGlyph g = info.getGlyphs().get(0);
if (g.getTextFallback() != null) {
    guiGraphics.drawString(font, g.getTextFallback(), x, y, 0xFFFFFFFF, false);
} else {
    ResourceLocation tex = ResourceLocation.parse(g.getTextureId());   // 1.20.x: new ResourceLocation(...)
    guiGraphics.blit(tex, x, y, size, size,
            g.getU(), g.getV(), g.getWidth(), g.getHeight(),
            g.getTextureWidth(), g.getTextureHeight());
}
```

For a chord (`LB + X`) the consumer inserts a `"+"` between adjacent glyphs, advancing by `font.width("+")` — the same approach `ControllerGlyphRenderer` uses internally.

### 5.6 Trigger mode enum

Mirrors the 13 values of `com.ifels.controlflex.binding.TriggerMode`:
`PRESS / RELEASE / HOLD / TAP / DELAY_PRESS / DELAY_HOLD / LONG_PRESS / LONG_HOLD / DOUBLE_PRESS / DOUBLE_TAP / DOUBLE_HOLD / DOUBLE_TOGGLE / TOGGLE`.

The implementation maps by `name()`; an unrecognised name (added later) falls back to `PRESS`, matching `TriggerMode.fromName`'s tolerance.

## 6. Implementation notes for the main repo

| Step | Reuse | Notes |
| --- | --- | --- |
| Context layer | Ready-made: `Minecraft.screen`, `BindingMapper.isOverlayPhaseActive()`, `BindingRegistry.getActiveLayer()` | Copy the order from `resolveTargetPhase()`: Screen → `gui`; else overlay phase → `overlay`; else `activeLayer`. **Do not read the `BindingMapper.phase` field** (no getter, and it updates a tick after the Screen opens, flashing one frame of the wrong key) |
| Binding lookup | `BindingRegistry.getEffectiveBinding(actionId, ctx.layer())` | Already includes `ForegroundBindingResolver` and per-Screen overrides; **do not rebuild it** |
| Shift modifier | `getEffectiveBinding(ActionId.LAYER_SHIFTn, LAYER_MAIN)` | Only when `ctx.layer()` is shiftN and something is bound there; do not use `GuideBindingResolver`'s AUTO — it keys off "shift equals main", which is guide semantics, not dispatch semantics |
| Skin | `GuideGlyphSkinResolver.resolveCached(...)` | The cache already exists; the API adds no second one; **must share one skin value with the display name**, see 6.1 |
| Button → cell | `ControllerGlyphMapper.resolveChar(skin, buttonId)` / `hasGlyph(...)` | |
| Cell → rectangle | `ControllerGlyphRenderer`'s `ATLAS_CELL=32`, `ATLAS_COLS=16`, `ATLAS_W=512`, `ATLAS_H=128` | Currently private; promote to package-visible constants or extract a static `atlasRect(slot)` |
| Display name | `ButtonDisplayNames.shortName(skin, buttonId, isPS5)`, concatenated per button | **Do not use `ActionBinding.getButtonsDisplayString()`**: it reads the static `GuideGlyphSkinAccess.current()`, which may already have switched controllers since the glyph resolution |
| Keyboard fallback | `BindingKeyboardIds.keyboardDisplayLabel(buttonId)` | Same criterion as `ControllerGlyphRenderer.textFallbackLabel`; when it returns null, fall back to the raw buttonId so "always drawable" holds |

### 6.1 The skin must be read once

Within a single query, the display name and the glyph cells must come from the same `GuideGlyphSkin` value, otherwise §2's promise ("buttons and glyphs never come from two skins") fails immediately. Two traps:

- `GuideManager.getCurrentGlyphSkin()` runs `persistLastSkinIfNeeded`, writing `cfx-client.json`; **it must not be called per frame**.
- `GuideGlyphSkinResolver` caches per instance; creating your own yields a different `lastResolved` than the global one.

Approach: the implementation holds one resolver instance fed by the same `gamepadSelector` and `glyphStyle` sources as `GuideManager`; if `GuideManager` has no side-effect-free accessor, add `peekGlyphSkin()` (read-only, no disk write) and take it from there.

Put the implementation in `com.ifels.controlflex.compat` (or a new `com.ifels.controlflex.api.impl`); the class name must start with `com.ifels.` because `ControlFlexApi` validates the package on injection.

Injection timing matches the existing providers: `ControlFlexApi.setActionBindingProvider(...)` during client init. When not injected, `isAvailable()` is unaffected (it keeps checking only the three legacy providers).

## 7. Versioning and release

1. `control-flex-api/gradle.properties`: `api_version` `0.9.2` → `0.9.2.1`.
2. `./tools/publish_maven_local.sh` publishes to `~/.m2` as `com.github.ControlFlexMC:control-flex-api:0.9.2.1` (the script verifies the POM is not a JitPack wrapper).
3. Consumers pin `0.9.2.1`. The main repo's `build.gradle.kts:103` already has `mavenLocal()` (added for local integration), so no repository change is needed.
4. The main repo's `gradle.properties` `cfx_api_version` moves to `0.9.2.1`.
5. Public docs (`docs/zh`, `docs/en` `api-reference.md`) gain a section when the implementation lands; this spec itself does not go into the public docs.

> **The re-publish window.** `0.9.2.1` was re-published in place once during review (the interface went from two methods to one). It is untagged with no external consumers, so re-publishing is safe; once it reaches JitPack, any further interface change must go through `0.9.2.2`.

## 8. Acceptance

This spec delivers only the API and the main-repo implementation, **no mod-specific action ids**. Cobblemon's four battle actions come from the bridge's own `KeyMapping` and compat config, which is a separate plan; for manual testing the existing compat entries (`epicfight` / `jei`) are enough.

- Unit: unbound / single button / chord / shift modifier — four shapes; `getGlyphs()` matches `getButtons()` in length and order; the modifier is first on a shift layer.
- Unit: a keyboard binding returns a non-empty `textFallback` and `getGlyphs()` still matches `getButtons()`.
- Unit: the same action resolves to different bindings under `gui` and `main` (layer isolation); an OVERLAY phase consults the `overlay` layer.
- Unit: an unbound shift modifier is not prepended; a bound one is.
- Unit: `null` / empty `actionId` returns an unbound info whose `getLayer()` still reports the consulted layer.
- Manual: bind a key to a GUI-layer `epicfight` action and confirm the returned rectangle blits the right glyph in game.
- Manual: switch the skin to PlayStation and re-query; glyphs become ✕○□△ and `getDisplayName()` changes with them (this verifies 6.1).
- Manual: after a rebind the **next frame** already reflects it (no cross-frame cache).

## 9. Open edges and known deviations

- **Shift latch vs hold is not distinguished.** On a shift layer `getButtons()` prepends the modifier, meaning "hold the modifier then press this". But a shift layer can also be latched (press once to stay), and while latched the bare button works — the HUD still shows two segments. The missing input is "is shift latched or held"; v1 does not ask.
- **The answer drifts with the phase.** Phase determination has a 3-tick debounce (`overlayEnterStreak` / `overlayExitStreak`), so for three ticks after closing an overlay the query may still report the `overlay` layer. That is the price of agreeing with dispatch, not a bug.
- Localised action labels ("Jump") are out of scope. `IActionBindingInfo` answers "which button, what to draw", not "what is this action called".
- **The API does not answer "does this action belong to the current screen".** An action bound on the global GUI layer resolves as bound from any screen. To scope it, use a per-Screen override (`user_screen_overrides.json`) so the binding exists only under that FQN; do not expect the API to filter.
- Per-glyph PNGs (pre-sliced at build time) are deferred; see the decision table.
- Overlays registered through `IInteractiveContextRegistrar` are now covered by `getActiveBinding`, but "is this overlay foreground" itself is not exposed; consumers decide for themselves whether to draw.

## 10. Open questions

Two items for the maintainer; they affect scope, not the API shape:

1. **Backfill scope.** The workspace holds 18 version trees; only `control-flex-1.20.1/api/` carries a vendored copy of the API sources, the rest (including 1.21.1 and stonecutter's neoforge / fabric / 1.21.11 / 26.x variants) consume it from JitPack. Does this plan touch only `control-flex-api` and the 1.21.1 main repo, or also the 1.20.1 copy?
2. **Release channel.** Does 0.9.2.1 go to mavenLocal only (local verification), or also get tagged and published to JitPack? A third-party bridge that pins `0.9.2.1` can only build once JitPack serves it.

## 11. Revision history

**First self-review (against the dispatch implementation) — the layer model was overturned**

- The "one method for GUI, one for game" premise was wrong: there are three phases (`InputPhase` = GAME / OVERLAY / GUI), and GAME uses `getActiveLayer()` (possibly shiftN). Two methods guarantee half the answers are wrong, and OVERLAY was not covered at all.
- Replaced by a single `getActiveBinding` whose context determination shares its source with the dispatcher; `getGuiBinding` / `getGameBinding` were deleted.
- Dropped `GuideBindingResolver`'s AUTO probe: it keys off "shift binding equals main", which is guide-display semantics, not dispatch semantics.
- Added a `BindingLayer` constant class so consumers stop building strings.

**Second self-review (implementation feasibility)**

- The context layer is computed from `resolveTargetPhase()`'s order (Screen → gui / overlay phase → overlay / else activeLayer) instead of reading `BindingMapper.phase`: that field has no getter, and the phase updates in the tick, one frame after the Screen opens, flashing a wrong key.
- Completed the unbound contract: `getLayer()` never empty, `getTriggerMode()` falls back to `PRESS`, `getActionId()` normalises null to an empty string.
- `getTextFallback()` gained a third fallback level (keyboard label → raw buttonId); otherwise "every button is drawable" fails on unknown keys.
- Added one limitation: the API does not answer "does this action belong to the current screen"; scoping relies on per-Screen overrides.
