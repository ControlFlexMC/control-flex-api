package com.ifels.controlflex.api;

/**
 * Per-screen extension point: one handler object per screen class, speaking for that class
 * only. Registered through {@link IScreenHandlerRegistry} against a fully-qualified class name.
 *
 * <p>Matching is <b>exact</b>: a subclass of a registered screen is not covered, which keeps the
 * blast radius of a handler to the one screen it was written for. At most one handler per class
 * — a second registration is rejected, so two bridges cannot silently fight over the same
 * screen.</p>
 *
 * <p><b>Thread safety</b>: called on the client main thread. Keep implementations short.</p>
 *
 * @since 0.13.0
 */
public interface IScreenHandler {

    /**
     * Consume the GUI "back" press for the registered screen before ControlFlex turns it into
     * Escape.
     *
     * @return {@code true} to consume the press (no Escape is sent); {@code false} to let
     *         ControlFlex handle back as usual
     */
    default boolean onBack() {
        return false;
    }

    /**
     * Declare that the registered screen's key hints are drawn by this handler, so ControlFlex
     * tracks a visible/hidden state for it and routes the built-in {@code hud_toggle} action to
     * {@link #onHudState}.
     *
     * <p>{@code false} (the default) means this screen has no toggleable HUD: pressing
     * {@code hud_toggle} on it does nothing and no state is persisted.</p>
     */
    default boolean ownsHud() {
        return false;
    }

    /**
     * The visibility for the registered screen changed — draw or stop drawing accordingly.
     *
     * <p>ControlFlex owns the state: it flips, persists and pushes. Implementations only store
     * the value (a plain field) and render from it; they must not write it back anywhere else.
     * Guaranteed to arrive before the first frame that depends on it: on the class becoming
     * foreground, on registration while already foreground, and right after a toggle.</p>
     *
     * @param visible whether the hints for this screen should be shown
     * @param reason  what caused this notification
     * @return {@code true} when the state was applied as asked. {@code false} means the handler
     *         could not apply it — ControlFlex then keeps the previous state (a toggle is not
     *         persisted, so what is stored never diverges from what the screen shows) and logs.
     */
    default boolean onHudState(boolean visible, HudStateReason reason) {
        return false;
    }
}
