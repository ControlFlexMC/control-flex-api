package com.ifels.controlflex.api;

/**
 * Installer passed to {@link IControlFlexPlugin} callbacks for exporting
 * compat and guide assets from the bridge mod's JAR into ControlFlex's
 * {@code cfx-mod/} directories.
 *
 * <p>ControlFlex provides the implementation; bridge mods simply call
 * {@link #install(String, String)} for each bundled asset.</p>
 *
 * @since 0.8.5
 */
public interface ICompatAssetInstaller {

    /**
     * Install a classpath resource from the calling plugin's JAR into the
     * target directory.
     *
     * @param resourcePath classpath resource path (e.g., "/assets/mymod/compat/epicfight_keys.json")
     * @param fileName     target file name in the destination directory
     * @return true if installed successfully, false if resource not found or I/O error
     */
    boolean install(String resourcePath, String fileName);
}
