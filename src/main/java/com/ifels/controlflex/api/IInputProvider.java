package com.ifels.controlflex.api;

import org.jetbrains.annotations.Nullable;

/**
 * Provides read-only access to the controller's hardware state.
 *
 * @since 0.8.5
 */
public interface IInputProvider {

    /** Check if a controller is currently connected and selected. */
    boolean isConnected();

    /**
     * Get the current controller state.
     *
     * <p>The returned object is a <b>read-only live view</b> that reflects
     * the latest polled values. Only read from the <b>client main thread</b>.
     * Do NOT cache the returned reference across ticks.</p>
     *
     * @return controller state view, or null if unavailable
     */
    @Nullable IControllerState getControllerState();

    /**
     * Get the name of the currently selected gamepad.
     * @return gamepad name (e.g., "Xbox Wireless Controller"), or null if none
     */
    @Nullable String getGamepadName();

    /**
     * Get the index of the currently selected gamepad.
     * @return gamepad index (0-based), or -1 if none
     */
    int getGamepadIndex();

    /**
     * Get the hardware capabilities of the currently connected controller.
     * Bridge mods can use this to adapt UI (e.g., show correct button glyphs).
     *
     * @return capabilities, or null if no controller is connected
     */
    @Nullable IControllerCapabilities getCapabilities();
}
