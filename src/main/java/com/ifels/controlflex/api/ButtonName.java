package com.ifels.controlflex.api;

/**
 * Well-known button and axis name constants for use with
 * {@link IControllerState#isButtonPressed(String)} and
 * {@link IControllerState#getAxisValue(String)}.
 *
 * <p>Use these constants instead of raw strings for compile-time safety
 * and IDE auto-completion.</p>
 *
 * @since 1.0.0
 */
public final class ButtonName {

    private ButtonName() {}

    // ===== Face Buttons =====
    public static final String A = "buttonA";
    public static final String B = "buttonB";
    public static final String X = "buttonX";
    public static final String Y = "buttonY";

    // ===== D-Pad =====
    public static final String DPAD_UP    = "dpadUp";
    public static final String DPAD_DOWN  = "dpadDown";
    public static final String DPAD_LEFT  = "dpadLeft";
    public static final String DPAD_RIGHT = "dpadRight";

    // ===== Bumpers =====
    public static final String LEFT_BUMPER  = "leftBumper";
    public static final String RIGHT_BUMPER = "rightBumper";

    // ===== Triggers (analog; also usable as digital with threshold) =====
    public static final String LEFT_TRIGGER  = "leftTrigger";
    public static final String RIGHT_TRIGGER = "rightTrigger";

    // ===== Stick Clicks =====
    public static final String LEFT_STICK_CLICK  = "leftStickClick";
    public static final String RIGHT_STICK_CLICK = "rightStickClick";

    // ===== Stick Directions (analog) =====
    public static final String LEFT_STICK_UP    = "leftStickUp";
    public static final String LEFT_STICK_DOWN  = "leftStickDown";
    public static final String LEFT_STICK_LEFT  = "leftStickLeft";
    public static final String LEFT_STICK_RIGHT = "leftStickRight";
    public static final String RIGHT_STICK_UP    = "rightStickUp";
    public static final String RIGHT_STICK_DOWN  = "rightStickDown";
    public static final String RIGHT_STICK_LEFT  = "rightStickLeft";
    public static final String RIGHT_STICK_RIGHT = "rightStickRight";

    // ===== Special Buttons =====
    public static final String BACK  = "buttonBack";
    public static final String START = "buttonStart";
    public static final String GUIDE = "buttonGuide";

    // ===== Paddles (elite controllers) =====
    public static final String PADDLE_1 = "paddle1";
    public static final String PADDLE_2 = "paddle2";
    public static final String PADDLE_3 = "paddle3";
    public static final String PADDLE_4 = "paddle4";

    // ===== Touchpad =====
    public static final String TOUCHPAD = "touchpad";

    // ===== Stick Axes (for use with getAxisValue) =====
    public static final String AXIS_LEFT_X  = "leftStickX";
    public static final String AXIS_LEFT_Y  = "leftStickY";
    public static final String AXIS_RIGHT_X = "rightStickX";
    public static final String AXIS_RIGHT_Y = "rightStickY";

    // ===== Trigger Axes (for use with getAxisValue) =====
    public static final String AXIS_LEFT_TRIGGER  = "leftTrigger";
    public static final String AXIS_RIGHT_TRIGGER = "rightTrigger";
}
