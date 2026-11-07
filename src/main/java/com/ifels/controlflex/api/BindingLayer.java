package com.ifels.controlflex.api;

/**
 * Input layer identifiers reported by {@link IActionBindingInfo#getLayer()}.
 *
 * <p>Use these constants instead of raw strings. Only one layer answers a query, and
 * which one depends on the foreground context — see
 * {@link IActionBindingProvider#getActiveBinding(String)}.</p>
 *
 * @since 0.9.2.1
 */
public final class BindingLayer {

    private BindingLayer() {}

    /** Layer used while a {@code Screen} is open. */
    public static final String GUI = "gui";

    /** Layer used while an interactive HUD overlay is in the foreground. */
    public static final String OVERLAY = "overlay";

    /** Base game layer. */
    public static final String MAIN = "main";

    /** Game layer variant while shift layer 1 is active. */
    public static final String SHIFT1 = "shift1";

    /** Game layer variant while shift layer 2 is active. */
    public static final String SHIFT2 = "shift2";

    /** Game layer variant while shift layer 3 is active. */
    public static final String SHIFT3 = "shift3";

    /** Game layer variant while shift layer 4 is active. */
    public static final String SHIFT4 = "shift4";
}
