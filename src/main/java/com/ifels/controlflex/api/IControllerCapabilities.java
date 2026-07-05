package com.ifels.controlflex.api;

/**
 * Read-only view of controller hardware capabilities.
 * Bridge mods can use this to adapt UI and behavior based on
 * which features the connected controller supports.
 *
 * <p>Obtain via {@link IInputProvider#getCapabilities()}.</p>
 *
 * @since 1.0.0
 */
public interface IControllerCapabilities {

    /** Controller type classification. */
    ControllerType getControllerType();

    /** Whether the controller has rear paddles. */
    boolean hasPaddles();

    /** Number of rear paddles (0 if none). */
    int getPaddleCount();

    /** Whether the controller has a clickable touchpad. */
    boolean hasTouchpad();

    /** Whether the controller supports gyroscope (motion sensors). */
    boolean hasGyro();

    /** Whether the controller supports haptic feedback (rumble). */
    boolean hasRumble();

    /** Whether the controller has analog triggers (vs. digital). */
    boolean hasAnalogTriggers();

    /** Whether the controller uses Nintendo-style face button layout (A/B swapped vs Xbox). */
    boolean isNintendoLayout();
}
