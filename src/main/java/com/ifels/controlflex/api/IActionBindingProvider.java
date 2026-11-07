package com.ifels.controlflex.api;

/**
 * Read-only queries for "which controller button will fire this action right now, and
 * what glyph should be drawn for it".
 *
 * <p>Obtain via {@link ControlFlexApi#getActionBindingProvider()}.</p>
 *
 * <p>Why a single call returns both: the buttons and the glyphs are two projections of
 * one resolution. Querying them separately can tear — a controller hot-plug or a glyph
 * skin change between the two calls yields a button label from one skin paired with an
 * icon from another.</p>
 *
 * <p><b>Thread safety</b>: client main thread only (tick or render). The result is a
 * snapshot of the resolution at call time. Do <b>not</b> cache it across frames: the
 * binding changes when the player rebinds, swaps controllers, switches shift layer, or
 * opens another screen.</p>
 *
 * @since 0.9.2.1
 */
public interface IActionBindingProvider {

    /**
     * Resolve {@code actionId} in the input context that is active right now.
     *
     * <p>The context is chosen the same way the input dispatcher chooses it:</p>
     * <ul>
     *   <li>a {@code Screen} is open → {@link BindingLayer#GUI}</li>
     *   <li>an interactive HUD overlay is foreground → {@link BindingLayer#OVERLAY}</li>
     *   <li>otherwise → the game layer, which is {@link BindingLayer#MAIN} or a shift
     *       layer ({@link BindingLayer#SHIFT1}..{@link BindingLayer#SHIFT4}) when the
     *       player switched to one</li>
     * </ul>
     *
     * <p>Per-screen user overrides apply automatically — the currently open screen is
     * part of the resolution, so callers do not pass a screen or a class name.</p>
     *
     * <p><b>Configured but unreachable reads as unbound.</b> The answer describes what a
     * press would do, not what is stored: if the action cannot be dispatched in the current
     * context (for a screen, an action missing from the GUI layer mapping), the result is
     * unbound even when a per-screen table names a button for it. Callers can therefore
     * draw a hint for every bound result without also asking whether it is reachable.</p>
     *
     * <p>When the resolved layer is a shift layer, the shift modifier's buttons are
     * reported <b>first</b>, followed by the action's own buttons — e.g.
     * {@code ["leftBumper", "buttonX"]}. This describes "hold the modifier, then press
     * the button". A latched shift layer (pressed once, stays active) is not
     * distinguished from a held one; the hint still shows both keys.</p>
     *
     * @param actionId action identifier, same format as {@link IActionStateProvider}
     *                 ({@code "modId:keyName"} for mod actions, plain names for built-ins).
     *                 {@code null} or empty resolves to an unbound info.
     * @return binding info, never {@code null} — check {@link IActionBindingInfo#isBound()}
     */
    IActionBindingInfo getActiveBinding(String actionId);
}
