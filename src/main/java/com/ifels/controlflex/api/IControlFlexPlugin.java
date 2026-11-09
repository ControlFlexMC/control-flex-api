package com.ifels.controlflex.api;

/**
 * SPI interface for bridge mods to integrate with ControlFlex.
 *
 * <p>Register via {@code META-INF/services/com.ifels.controlflex.api.IControlFlexPlugin}.</p>
 *
 * @since 0.8.5
 */
public interface IControlFlexPlugin {

    /**
     * The mod ID of the bridge mod implementing this plugin.
     * Used for logging and conflict detection.
     */
    String getModId();

    /**
     * Called when ControlFlex is fully initialized and all API providers
     * are available. Safe to query all API methods including controller state.
     *
     * <p>Before this callback, {@link #onInstallCompatConfigs} and
     * {@link #onInstallGuideAssets} have already been called, so compat
     * and guide files are in place on disk.</p>
     *
     * <p>Implementation suggestions:
     * <ol>
     *   <li>Call {@link #requireApiVersion(String)} for version checking</li>
     *   <li>Register event listeners</li>
     *   <li>Push initial states to {@link IPlayerStateRegistry}</li>
     * </ol>
     */
    default void onControlFlexReady() {}

    /**
     * Called when the controller connection state changes.
     *
     * @param connected true if a controller is now connected, false if disconnected
     */
    default void onControllerConnectionChanged(boolean connected) {}

    /**
     * Export compat configuration files from this bridge mod's JAR into the
     * {@code mods/} directory. ControlFlex calls this before
     * {@link #onControlFlexReady()}.
     *
     * <p>Use the provided {@link ICompatAssetInstaller} to install bundled assets:</p>
     * <pre>{@code
     * installer.install("/assets/mymod/compat/epicfight_keys.json", "epicfight_keys.json");
     * }</pre>
     *
     * @param installer handles classpath lookup and file copy
     */
    default void onInstallCompatConfigs(ICompatAssetInstaller installer) {}

    /**
     * Export guide configuration files from this bridge mod's JAR.
     * Same pattern as {@link #onInstallCompatConfigs}.
     *
     * @param installer handles classpath lookup and file copy
     */
    default void onInstallGuideAssets(ICompatAssetInstaller installer) {}

    /**
     * Optionally provide a camera look consumer (e.g., bridge mods whose
     * camera is taken over by Epic Fight).
     *
     * <p>When a non-null consumer is returned, ControlFlex asks
     * {@link ICameraLookConsumer#consumeLook} before writing player rotation
     * on direct / smooth-direct look; returning true skips the write
     * (typically the consumer forwards the delta to the taking-over mod's
     * camera API). "Simulated mouse movement" is unaffected (it goes through
     * the vanilla mouse pipeline, which the taking-over mod intercepts
     * itself).</p>
     *
     * @return camera consumer, or null (default: no takeover)
     * @since 0.8.8
     */
    default ICameraLookConsumer cameraLookConsumer() {
        return null;
    }

    /**
     * Declare actions that only ControlFlex sees, before the binding profile is loaded.
     *
     * <p>Called after {@link #onInstallCompatConfigs} / {@link #onInstallGuideAssets} and before
     * {@link #onControlFlexReady()}. The registrar is valid for the duration of this call only;
     * keeping a reference and registering later is a programming error and throws.</p>
     *
     * <p>Registered actions are not {@code KeyMapping}s: they never reach the vanilla controls
     * screen and never write global key state. Their bindings live in ControlFlex's own storage,
     * and their default values are consulted only while the user has not set one.</p>
     *
     * @param registrar registration window, valid only inside this callback
     * @since 0.9.2.2
     */
    default void onRegisterActions(IActionRegistrar registrar) {
    }

    /**
     * One of this plugin's registered actions fired.
     *
     * <p>Called on the client main thread inside the input tick, before the frame that depends on
     * it is rendered. Fires on the activation edge: once per press for one-shot modes, once when
     * a hold starts, once per toggle-on. Exceptions are isolated and logged.</p>
     *
     * @since 0.9.2.2
     */
    default void onActionTriggered(ActionTrigger trigger) {
    }

    /**
     * A hold-style or latching registered action became inactive.
     *
     * <p>One-shot modes (press, tap, release, double press, …) never produce this callback.</p>
     *
     * @param actionId the action that ended
     * @since 0.9.2.2
     */
    default void onActionReleased(String actionId) {
    }

    /**
     * Convenience method: check whether the ControlFlex API version meets
     * a minimum requirement. Logs a warning and returns false if not.
     *
     * @param minVersion minimum required API version (e.g., "0.8.7")
     * @return true if the API is available and version is sufficient
     */
    default boolean requireApiVersion(String minVersion) {
        if (!ControlFlexApi.isAvailable()) {
            return false;
        }
        String current = ControlFlexApi.getApiVersion();
        if (current == null) return false;

        String[] curParts = current.split("\\.");
        String[] minParts = minVersion.split("\\.");
        int len = Math.max(curParts.length, minParts.length);
        for (int i = 0; i < len; i++) {
            int c = i < curParts.length ? ControlFlexApi.parseVersionPart(curParts[i]) : 0;
            int m = i < minParts.length ? ControlFlexApi.parseVersionPart(minParts[i]) : 0;
            if (c != m) return c >= m;
        }
        return true;
    }
}
