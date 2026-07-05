package com.ifels.controlflex.api;

import java.io.File;

/**
 * SPI interface for bridge mods to integrate with ControlFlex.
 *
 * <p>Register via {@code META-INF/services/com.ifels.controlflex.api.IControlFlexPlugin}.</p>
 *
 * @since 1.0.0
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
     * {@code cfx-mod/} directory. ControlFlex calls this before
     * {@link #onControlFlexReady()}.
     *
     * <p>Use {@link #installAsset} to copy files from this plugin's JAR:</p>
     * <pre>{@code
     * installAsset(cfxModDir, "/assets/mymod/compat/epicfight_keys.json", "epicfight_keys.json");
     * }</pre>
     *
     * @param cfxModDir config/controlflex/compat/cfx-mod/ directory (guaranteed to exist)
     */
    default void onInstallCompatConfigs(File cfxModDir) {}

    /**
     * Export guide configuration files from this bridge mod's JAR into the
     * {@code cfx-mod/} directory. ControlFlex calls this before
     * {@link #onControlFlexReady()}.
     *
     * <p>Same pattern as {@link #onInstallCompatConfigs}. Use {@link #installAsset}.</p>
     *
     * @param cfxModDir config/controlflex/guides/cfx-mod/ directory (guaranteed to exist)
     */
    default void onInstallGuideAssets(File cfxModDir) {}

    /**
     * Convenience method: check whether the ControlFlex API version meets
     * a minimum requirement. Logs a warning and returns false if not.
     *
     * @param minVersion minimum required API version (e.g., "1.0.0")
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
            int c = i < curParts.length ? parseVersionPart(curParts[i]) : 0;
            int m = i < minParts.length ? parseVersionPart(minParts[i]) : 0;
            if (c != m) return c >= m;
        }
        return true;
    }

    /** Parse a version segment; non-numeric segments are treated as 0. */
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
