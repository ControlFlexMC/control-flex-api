package com.ifels.controlflex.api;

/**
 * Interactive-context registrar (spec 主 §10): mods actively notify entering an
 * interactive overlay/screen (with its class name) and may declare the desired
 * stick behavior. Pair with {@link #notifyClose(String)}; ControlFlex also
 * auto-clears on phase exit as a fallback.
 *
 * <p>Thread safety: call from the client (main) thread only. When ControlFlex is
 * not installed, {@code ControlFlexApi.getInteractiveContextRegistrar()} returns
 * null — callers must null-check.</p>
 *
 * @since 0.8.6
 */
public interface IInteractiveContextRegistrar {

    /** Notifies entry into an interactive overlay/screen (className required); pair with notifyClose. */
    void notifyOpen(InteractiveContextHint hint);

    /** Paired exit API: notifies leaving that context. */
    void notifyClose(String className);

    /** Clears everything (fallback for mod unload / world exit scenarios). */
    void clearAll();
}
