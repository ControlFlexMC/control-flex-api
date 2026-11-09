package com.ifels.controlflex.api;

/**
 * The part of ControlFlex an action belongs to.
 *
 * <p>An action may declare several layers. Declaring none means "all three" — the same
 * behaviour as an unconfigured binding.</p>
 *
 * @since 0.9.2.2
 */
public enum ActionLayer {

    /** The game itself: main and the four shift layers. */
    IN_GAME,

    /** A {@code Screen} being open. */
    SCREEN,

    /** An interactive HUD overlay in the foreground, without a screen. */
    OVERLAY
}
