package com.ifels.controlflex.api;

import java.util.List;

/**
 * Resolution result for one action: which buttons are bound, how to draw them, and
 * which input layer the answer came from.
 *
 * <p>A value object — it reflects the state at the moment of the query and is not
 * updated afterwards.</p>
 *
 * @since 0.9.2.1
 */
public interface IActionBindingInfo {

    /** The action this info describes; empty string when a null/empty id was queried. */
    String getActionId();

    /** Whether at least one button is bound. */
    boolean isBound();

    /**
     * Bound button identifiers, in press order.
     *
     * <p>Values use the same identifiers as {@link ButtonName} (for example
     * {@code "buttonA"}, {@code "leftBumper"}). Keyboard keys may also appear when the
     * player bound a keyboard key instead of a controller button; those have no glyph
     * artwork and are drawn via {@link IControllerGlyph#getTextFallback()}.</p>
     *
     * <p>Shift combos are <b>flattened</b>: the modifier comes first, then the action's
     * own buttons. Empty when unbound.</p>
     *
     * @return unmodifiable list, never {@code null}
     */
    List<String> getButtons();

    /**
     * Human-readable label for the whole combo, already localized for the active glyph
     * skin — {@code "A"}, {@code "LB + X"}, {@code "F1"}.
     *
     * <p>Empty string when unbound.</p>
     */
    String getDisplayName();

    /**
     * One drawable glyph per entry of {@link #getButtons()}, same order and size.
     *
     * <p>Every entry is drawable: either a texture rectangle or a
     * {@link IControllerGlyph#getTextFallback() text fallback}.</p>
     *
     * @return unmodifiable list, never {@code null}; empty when unbound
     */
    List<IControllerGlyph> getGlyphs();

    /**
     * The input layer this answer came from — see {@link BindingLayer}.
     *
     * <p>Reported <b>even when unbound</b>: it names the layer that was consulted, so a
     * caller can tell "not bound here" from "not bound anywhere". Never empty.</p>
     */
    String getLayer();

    /**
     * How the binding is triggered.
     *
     * <p>{@link TriggerMode#PRESS} when unbound.</p>
     */
    TriggerMode getTriggerMode();
}
