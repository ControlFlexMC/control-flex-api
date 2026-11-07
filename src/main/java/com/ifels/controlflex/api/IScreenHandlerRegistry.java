package com.ifels.controlflex.api;

import org.jetbrains.annotations.Nullable;

/**
 * Registers {@link IScreenHandler}s for specific screen classes, and keeps the per-class HUD
 * visibility state those handlers are notified about.
 *
 * <p>The key is a fully-qualified class name, not a class object, because this API has no
 * Minecraft dependency — which also lets overlays register under the class name they report
 * when they become foreground.</p>
 *
 * <p><b>Thread safety</b>: client main thread.</p>
 *
 * @since 0.13.0
 */
public interface IScreenHandlerRegistry {

    /**
     * Register a handler for one screen class.
     *
     * @param screenClassName fully-qualified screen or overlay class, e.g.
     *                        {@code "com.example.ModScreen"}
     * @param handler         invoked for that class's back press and HUD state
     * @return {@code true} if registered; {@code false} if the class already has a handler or
     *         the arguments are invalid
     */
    boolean register(String screenClassName, IScreenHandler handler);

    /**
     * Remove the handler for a screen class.
     *
     * @return {@code true} if a handler was removed
     */
    boolean unregister(String screenClassName);

    /**
     * @return the handler registered for that class, or {@code null} — useful for diagnosing
     *         "why is my back press not consumed"
     */
    @Nullable IScreenHandler registeredFor(String screenClassName);

    /**
     * Current HUD visibility for a screen class. Never called for rendering — the state is
     * pushed to the handler via {@link IScreenHandler#onHudState}; this is for diagnostics and
     * for code that draws for several classes at once.
     *
     * @return {@code true} unless a toggle has stored {@code false} for this class
     */
    boolean isHudVisible(String screenClassName);
}
