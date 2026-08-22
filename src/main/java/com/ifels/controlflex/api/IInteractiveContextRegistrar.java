package com.ifels.controlflex.api;

/**
 * Interactive-context registrar (spec 主 §10): mods actively notify ControlFlex
 * when they bring an interactive overlay/screen to the <b>foreground</b> (declared
 * by its class name) and when they return to the <b>background</b>. ControlFlex
 * uses this to switch stick behavior while such a context is active; it also
 * auto-clears on phase exit as a fallback.
 *
 * <p><b>Foreground/background pairing</b>: call
 * {@link #notifyForeground(String)} when the interactive context opens, and the
 * paired {@link #notifyBackground(String)} with the same class name when it
 * closes. Both take the fully-qualified class name of the interactive
 * screen/overlay — that is all ControlFlex needs to identify the context;</p>
 * stick behavior for the class is resolved from ControlFlex compat
 * configuration.</p>
 *
 * <p>Thread safety: call from the client (main) thread only. When ControlFlex is
 * not installed, {@code ControlFlexApi.getInteractiveContextRegistrar()} returns
 * null — callers must null-check.</p>
 *
 * @since 0.8.6
 */
public interface IInteractiveContextRegistrar {

    /**
     * Notifies that an interactive overlay/screen (className) came to the foreground.
     * Pair with {@link #notifyBackground(String)}; ControlFlex also auto-clears on
     * phase exit as a fallback.
     *
     * @param className fully-qualified class name of the interactive context
     */
    void notifyForeground(String className);

    /**
     * Paired exit API: notifies that the context (className) returned to the background.
     *
     * @param className same class name passed to {@link #notifyForeground(String)}
     */
    void notifyBackground(String className);

    /** Clears everything (fallback for mod unload / world exit scenarios). */
    void clearAll();
}
