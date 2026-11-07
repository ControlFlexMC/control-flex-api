package com.ifels.controlflex.api;

/**
 * Where a HUD visibility change came from, passed to {@link IScreenHandler#onHudState}.
 *
 * <p>New values may be appended in future versions; implementations switching over this enum
 * must carry a {@code default} branch.</p>
 *
 * @since 0.13.0
 */
public enum HudStateReason {

    /** The class was seen for the first time — no stored value, so the default (visible) applies. */
    DEFAULT,

    /** The stored value from a previous session was restored. */
    RESTORED,

    /** The player pressed the hud_toggle action. */
    USER_TOGGLE
}
