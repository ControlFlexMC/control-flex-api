package com.ifels.controlflex.api;

/**
 * How a bound action is triggered.
 *
 * <p>Mirrors the binding modes offered by ControlFlex's binding editor. The timing
 * modes follow the press timeline {@code tap < delay < long press}; at the delay and
 * long-press boundaries a binding either fires once or activates and stays active until
 * release.</p>
 *
 * <p>Names match the internal modes one-to-one. A name this enum does not know about
 * (added by a newer ControlFlex) is reported as {@link #PRESS}.</p>
 *
 * @since 0.10.0
 */
public enum TriggerMode {

    /** Fires once when the button goes down. */
    PRESS,

    /** Fires once when the button is released. */
    RELEASE,

    /** Active for as long as the button is held. */
    HOLD,

    /** Fires once on release after a short press; long presses do nothing on release. */
    TAP,

    /** Fires once when the press outlives the tap window. */
    DELAY_PRESS,

    /** Activates at the delay threshold and stays active until release. */
    DELAY_HOLD,

    /** Fires once after the long-press threshold. */
    LONG_PRESS,

    /** Activates at the long-press threshold and stays active until release. */
    LONG_HOLD,

    /** Fires on the second press inside the tap window. */
    DOUBLE_PRESS,

    /** Fires on the second release of two short presses. */
    DOUBLE_TAP,

    /** Short press then press-and-hold inside the tap window; held until release. */
    DOUBLE_HOLD,

    /** Latches on the second short-press release. */
    DOUBLE_TOGGLE,

    /** Alternates between active and inactive on each press. */
    TOGGLE
}
