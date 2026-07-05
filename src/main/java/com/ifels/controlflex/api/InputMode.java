package com.ifels.controlflex.api;

/**
 * Describes the current input mode of the player.
 *
 * <p>Bridge mods can use this to determine whether to use
 * controller or keyboard/mouse input paths.</p>
 *
 * @since 0.8.5
 */
public enum InputMode {
    /** Player is using keyboard and mouse exclusively. */
    KEYBOARD_MOUSE,

    /** Player has a controller connected. */
    MIXED
}
