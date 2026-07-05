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
     * Convenience method: check whether the ControlFlex API version meets
     * a minimum requirement. Logs a warning and returns false if not.
     *
     * @param minVersion minimum required API version (e.g., "0.8.5")
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
