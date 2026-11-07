package com.ifels.controlflex.api;

/**
 * Places the virtual mouse cursor.
 *
 * <p>Bridge mods use this to make a hand-drawn highlight and the real cursor agree: after moving
 * their own selection, warping the cursor onto the selected element lets the built-in
 * {@code gui_press} action activate it — no per-element confirm action needed.</p>
 *
 * <p><b>Thread safety</b>: client main thread only. Does nothing harmful when no screen is open
 * or the cursor is not controller-driven; the coordinates are simply stored.</p>
 *
 * @since 0.11.0
 */
public interface ICursorController {

    /**
     * Move the cursor to a GUI-scaled position — the same coordinate space as
     * {@code Screen} mouse callbacks and {@code GuiGraphics} drawing.
     *
     * @param guiX GUI-scaled X
     * @param guiY GUI-scaled Y
     */
    void warpToGui(double guiX, double guiY);

    /**
     * Whether the cursor is currently driven by a controller. When false, the player is on
     * keyboard/mouse and warping only moves the OS pointer.
     */
    boolean isActive();
}
