package com.ifels.controlflex.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * @since 0.8.5
 */
public final class ControlFlexApi {

    private static volatile IActionStateProvider actionStateProvider;
    private static volatile IInputProvider inputProvider;
    private static volatile IPlayerStateRegistry playerStateRegistry;
    private static volatile GuideReloadCallback guideReloadCallback;
    private static volatile String apiVersion;

    // Plugin registry — bridge mods register themselves instead of being discovered via ServiceLoader
    private static final List<IControlFlexPlugin> registeredPlugins = new ArrayList<>();

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
     * @since 0.8.5
     */
    public static IPlayerStateRegistry getPlayerStateRegistry() {
        return playerStateRegistry;
    }

    /**
     * Get the API version string (e.g., "0.8.5").
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

    // ===== PLUGIN REGISTRATION =====

    /**
     * Register a bridge mod plugin with ControlFlex.
     *
     * <p>Bridge mods should call this during their initialization (e.g., in
     * {@code onInitialize} for Fabric or {@code FMLClientSetupEvent} for Forge).
     * ControlFlex will export compat/guide assets and call
     * {@link IControlFlexPlugin#onControlFlexReady()} when ready.</p>
     *
     * <p>Late registration is supported: if ControlFlex is already initialized
     * when this is called, {@code onControlFlexReady()} is invoked immediately.</p>
     *
     * @param plugin the bridge mod plugin to register
     * @throws SecurityException if the plugin is not from the ControlFlex package
     * @since 0.9.0
     */
    public static void registerPlugin(IControlFlexPlugin plugin) {
        Objects.requireNonNull(plugin,
            "[ControlFlexApi] Plugin must not be null.");
        String className = plugin.getClass().getName();
        if (!className.startsWith("com.ifels.controlflex.") &&
            !className.startsWith("com.ifels.cfx.")) {
            throw new SecurityException(
                "[ControlFlexApi] Rejected plugin from unauthorized package: " +
                className + ". Bridge mods should use the com.ifels.cfx.* package.");
        }
        synchronized (registeredPlugins) {
            registeredPlugins.add(plugin);
        }
        // If ControlFlex is already ready, notify immediately so late
        // registrations don't miss the onControlFlexReady callback.
        if (isAvailable()) {
            try {
                plugin.onControlFlexReady();
            } catch (Exception e) {
                // Swallow — bridge mod's problem, don't break the caller
            }
        }
    }

    /**
     * Get all registered bridge mod plugins.
     *
     * <p>Called by the ControlFlex implementation during plugin processing.
     * Returns a snapshot copy; modifications to the returned list do not
     * affect the internal registry.</p>
     *
     * @return unmodifiable list of registered plugins
     * @since 0.9.0
     */
    public static List<IControlFlexPlugin> getRegisteredPlugins() {
        synchronized (registeredPlugins) {
            return Collections.unmodifiableList(new ArrayList<>(registeredPlugins));
        }
    }

    // ===== INTERNAL — DO NOT CALL FROM BRIDGE MODS =====
    // Third-party mods calling these methods will cause the API to become
    // permanently unavailable. These are called exactly once by ControlFlex
    // during initialization.

    /**
     * Verify that a provider implementation comes from the ControlFlex package.
     * Rejects implementations from other mods that might try to inject fake providers.
     */
    private static void verifyControlFlexImpl(Object provider, String name) {
        String className = provider.getClass().getName();
        if (!className.startsWith("com.ifels.controlflex.")) {
            throw new SecurityException(
                "[ControlFlexApi] Rejected " + name + " from unauthorized package: " +
                className + ". Only ControlFlex implementations are accepted.");
        }
    }

    /** @internal */
    public static void setActionStateProvider(IActionStateProvider provider) {
        Objects.requireNonNull(provider,
            "[ControlFlexApi] ActionStateProvider must not be null. " +
            "This is an internal method — do not call from bridge mods.");
        verifyControlFlexImpl(provider, "ActionStateProvider");
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
        verifyControlFlexImpl(provider, "InputProvider");
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
        verifyControlFlexImpl(registry, "PlayerStateRegistry");
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

    // ===== TESTING ONLY — DO NOT CALL FROM BRIDGE OR IMPLEMENTATION MODS =====

    /**
     * Reset all providers to null. For unit testing only.
     * Calling this in production code will break the API for all consumers.
     *
     * @internal
     */
    static void resetForTesting() {
        actionStateProvider = null;
        inputProvider = null;
        playerStateRegistry = null;
        apiVersion = null;
        guideReloadCallback = null;
        synchronized (registeredPlugins) {
            registeredPlugins.clear();
        }
    }

    /** @internal Bridge for guide reload from plugins. */
    @FunctionalInterface
    public interface GuideReloadCallback {
        void reloadGuides();
    }
}
