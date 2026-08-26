package com.ifels.controlflex.api;

/**
 * Interactive-context registrar (spec 主 §10): mods actively notify ControlFlex
 * when they bring an interactive <b>overlay</b> to the <b>foreground</b> (declared
 * by its class name) and when it returns to the <b>background</b>. ControlFlex
 * records the class as an active overlay and switches stick behavior during the
 * OVERLAY phase while it is active; it also auto-clears on phase exit as a
 * fallback.
 *
 * <p><b>Foreground/background pairing</b>: call
 * {@link #notifyOverlayForeground(String)} when the overlay opens, and the
 * paired {@link #notifyOverlayBackground(String)} with the same class name when
 * it closes. Both take the fully-qualified class name of the overlay — that is
 * all ControlFlex needs to identify the context; stick behavior for the class is
 * resolved from ControlFlex compat configuration.
 *
 * <p>Thread safety: call from the client (main) thread only. When ControlFlex is
 * not installed, {@code ControlFlexApi.getInteractiveContextRegistrar()} returns
 * null — callers must null-check.
 *
 * @since 0.8.6
 */
public interface IInteractiveContextRegistrar {

    /**
     * Notifies that an interactive overlay (className) came to the foreground,
     * becoming the active overlay for the OVERLAY phase.
     * Pair with {@link #notifyOverlayBackground(String)}; ControlFlex also
     * auto-clears on phase exit as a fallback.
     *
     * @param className fully-qualified class name of the interactive overlay
     */
    void notifyOverlayForeground(String className);

    /**
     * Paired exit API: notifies that the overlay (className) returned to the background.
     *
     * @param className same class name passed to {@link #notifyOverlayForeground(String)}
     */
    void notifyOverlayBackground(String className);

    /** Clears everything (fallback for mod unload / world exit scenarios). */
    void clearAll();
}
