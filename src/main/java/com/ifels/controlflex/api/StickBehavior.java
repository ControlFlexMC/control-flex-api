package com.ifels.controlflex.api;

/**
 * Stick behavior modes (isomorphic with the compat StickModeEntry.behavior values).
 *
 * @since 0.8.6
 */
public enum StickBehavior {
    /** Default behavior (no override). */
    DEFAULT,
    /** Stick positions the cursor radially (center + direction * radius) for ring selection. */
    RADIAL_PATH,
    /** Stick drives the virtual mouse cursor directly. */
    VIRTUAL_MOUSE,
    /** Stick does not drive the cursor. */
    DISABLED
}
