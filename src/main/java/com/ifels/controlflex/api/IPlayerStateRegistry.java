package com.ifels.controlflex.api;

/**
 * Registry for third-party mod states that can influence ControlFlex behavior.
 *
 * <p>Bridge mods use this to inform ControlFlex about the state of the mods they bridge.
 * State keys follow the format {@code "modId:stateName"}, e.g. {@code "epicfight:battle_mode"}.</p>
 *
 * <p><b>Thread safety</b>: {@link #setState} may be called from any thread;
 * {@link #getState} should be read from the client thread for consistent results.</p>
 *
 * @since 1.0.0
 */
public interface IPlayerStateRegistry {

    /**
     * Register or update a player state.
     *
     * @param stateKey the state identifier (e.g., "epicfight:battle_mode")
     * @param active   true if the player is currently in this state
     */
    void setState(String stateKey, boolean active);

    /**
     * Query a player state.
     *
     * <p><b>Note</b>: Returns false for both "not registered" and
     * "registered but inactive" states. Callers cannot distinguish between
     * these two cases.</p>
     *
     * @param stateKey the state identifier
     * @return true if active; false if inactive or not registered
     */
    boolean getState(String stateKey);

    /**
     * Remove a player state registration entirely.
     * NOT equivalent to {@code setState(stateKey, false)} which keeps the key
     * present with value false.
     *
     * @param stateKey the state identifier to remove
     */
    void clearState(String stateKey);
}
