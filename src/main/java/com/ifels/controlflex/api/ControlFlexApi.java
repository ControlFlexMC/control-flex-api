package com.ifels.controlflex.api;

import java.util.Objects;

/**
 * ControlFlex public API entry point.
 * Bridge mods should use this class as the sole access to ControlFlex functionality.
 *
 * <p>All methods return {@code null} (or empty/false for primitives) when
 * ControlFlex is not installed or not yet initialized.
 * Always check {@link #isAvailable()} before calling other methods in
 * initialization code.</p>
 *
 * <p>Thread safety: all methods must be called from the client thread
 * (same thread as Minecraft's game loop), unless otherwise noted.</p>
 *
 * @since 1.0.0
 */
public final class ControlFlexApi {

    private static volatile IActionStateProvider actionStateProvider;
    private static volatile IInputProvider inputProvider;
    private static volatile IPlayerStateRegistry playerStateRegistry;
    private static volatile GuideReloadCallback guideReloadCallback;
    private static volatile String apiVersion;

    private ControlFlexApi() {}

    /**
     * Check if ControlFlex is installed and initialized.
     * Returns true only when all providers are injected.
     */
    public static boolean isAvailable() {
        return actionStateProvider != null && inputProvider != null && playerStateRegistry != null;
    }

    /**
     * Check if a controller is currently connected.
     */
    public static boolean isControllerConnected() {
        IInputProvider provider = inputProvider;
        return provider != null && provider.isConnected();
    }

    /**
     * Get the action state provider for querying game/GUI action states.
     * @return provider instance, or null if ControlFlex is not available
     */
    public static IActionStateProvider getActionStateProvider() {
        return actionStateProvider;
    }

    /**
     * Get the input provider for querying controller hardware state.
     * @return provider instance, or null if ControlFlex is not available
     */
    public static IInputProvider getInputProvider() {
        return inputProvider;
    }

    /**
     * Get the player state registry for third-party mod states.
     *
     * @return registry instance, or null if ControlFlex is not available
     * @since 1.0.0
     */
    public static IPlayerStateRegistry getPlayerStateRegistry() {
        return playerStateRegistry;
    }

    /**
     * Get the API version string (e.g., "1.0.0").
     */
    public static String getApiVersion() {
        return apiVersion;
    }

    /**
     * Reload guide definitions from {@code config/controlflex/guides/}.
     * Bridge mods should call this after installing or updating bundled guide JSON.
     */
    public static void reloadGuides() {
        GuideReloadCallback callback = guideReloadCallback;
        if (callback != null) {
            callback.reloadGuides();
        }
    }

    // ===== INTERNAL — DO NOT CALL FROM BRIDGE MODS =====
    // Third-party mods calling these methods will cause the API to become
    // permanently unavailable. These are called exactly once by ControlFlex
    // during initialization.

    /** @internal */
    public static void setActionStateProvider(IActionStateProvider provider) {
        Objects.requireNonNull(provider,
            "[ControlFlexApi] ActionStateProvider must not be null. " +
            "This is an internal method — do not call from bridge mods.");
        if (actionStateProvider != null) {
            throw new IllegalStateException(
                "[ControlFlexApi] ActionStateProvider already set. " +
                "ControlFlexApi.setXxx methods are internal. " +
                "Use ControlFlexApi.getXxx() for public API access.");
        }
        actionStateProvider = provider;
    }

    /** @internal */
    public static void setInputProvider(IInputProvider provider) {
        Objects.requireNonNull(provider,
            "[ControlFlexApi] InputProvider must not be null. " +
            "This is an internal method — do not call from bridge mods.");
        if (inputProvider != null) {
            throw new IllegalStateException(
                "[ControlFlexApi] InputProvider already set. " +
                "ControlFlexApi.setXxx methods are internal. " +
                "Use ControlFlexApi.getXxx() for public API access.");
        }
        inputProvider = provider;
    }

    /** @internal */
    public static void setPlayerStateRegistry(IPlayerStateRegistry registry) {
        Objects.requireNonNull(registry,
            "[ControlFlexApi] PlayerStateRegistry must not be null. " +
            "This is an internal method — do not call from bridge mods.");
        if (playerStateRegistry != null) {
            throw new IllegalStateException(
                "[ControlFlexApi] PlayerStateRegistry already set. " +
                "ControlFlexApi.setXxx methods are internal. " +
                "Use ControlFlexApi.getXxx() for public API access.");
        }
        playerStateRegistry = registry;
    }

    /** @internal */
    public static void setApiVersion(String version) {
        apiVersion = version;
    }

    /** @internal */
    public static void setGuideReloadCallback(GuideReloadCallback callback) {
        guideReloadCallback = callback;
    }

    /** @internal Bridge for guide reload from plugins. */
    @FunctionalInterface
    public interface GuideReloadCallback {
        void reloadGuides();
    }
}
