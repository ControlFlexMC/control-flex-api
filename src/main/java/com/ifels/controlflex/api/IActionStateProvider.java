package com.ifels.controlflex.api;

import java.util.Set;

/**
 * Provides read-only access to ControlFlex's action state tracking.
 *
 * <p>Action IDs follow the format {@code "modId:keyName"}, e.g.,
 * {@code "epicfight:key.epicfight.attack"} for mod keys, or
 * {@code "key.jump"} / {@code "attack"} for vanilla and built-in actions.</p>
 *
 * @since 0.8.5
 */
public interface IActionStateProvider {

    /**
     * Check if a game-layer action is currently active (held/triggered).
     *
     * @param actionId the action identifier
     * @return true if the action is active
     */
    boolean isGameActionActive(String actionId);

    /**
     * Check if a GUI-layer action is currently active.
     *
     * @param actionId the action identifier
     * @return true if the action is active in GUI context
     */
    boolean isGuiActionActive(String actionId);

    /**
     * Get all currently active game-layer action IDs.
     *
     * <p>The returned set is an unmodifiable <b>live view</b> — its contents
     * change each tick. Do NOT cache across ticks.</p>
     *
     * @return unmodifiable live view of active game action IDs
     */
    Set<String> getActiveGameActions();

    /**
     * Get all currently active GUI-layer action IDs.
     *
     * <p>The returned set is an unmodifiable <b>live view</b> — its contents
     * change each tick. Do NOT cache across ticks.</p>
     *
     * @return unmodifiable live view of active GUI action IDs
     */
    Set<String> getActiveGuiActions();
}
