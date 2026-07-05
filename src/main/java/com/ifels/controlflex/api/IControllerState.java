package com.ifels.controlflex.api;

/**
 * Read-only live view of a controller's hardware state.
 *
 * <p>Axis values are normalized to {@code -1.0} to {@code 1.0}.
 * Trigger values are normalized to {@code 0.0} to {@code 1.0}.
 * Y-axis: up is negative, down is positive (screen coordinate convention).</p>
 *
 * <p><b>Important</b>: This is NOT a snapshot. The underlying state changes
 * each tick. Only read from the client main thread. Do not cache references
 * across ticks.</p>
 *
 * <p><b>Thread safety</b>: Individual field reads are atomic and visible
 * across threads (backed by volatile fields). However, there is no consistency
 * guarantee across multiple fields — successive reads of different fields may
 * reflect different poll ticks.</p>
 *
 * @since 0.8.5
 */
public interface IControllerState {

    /** @return true if the controller is connected */
    boolean isConnected();

    // ===== Analog Sticks =====

    /** @return left stick X axis (-1.0 = full left, 1.0 = full right) */
    float getLeftStickX();
    /** @return left stick Y axis (-1.0 = full up, 1.0 = full down) */
    float getLeftStickY();
    /** @return right stick X axis (-1.0 = full left, 1.0 = full right) */
    float getRightStickX();
    /** @return right stick Y axis (-1.0 = full up, 1.0 = full down) */
    float getRightStickY();

    // ===== Triggers =====

    /** @return left trigger value (0.0 = released, 1.0 = fully pressed) */
    float getLeftTrigger();
    /** @return right trigger value (0.0 = released, 1.0 = fully pressed) */
    float getRightTrigger();

    /** @return true if left trigger is pressed beyond threshold (default 0.5) */
    default boolean isLeftTriggerPressed() { return getLeftTrigger() > 0.5f; }
    /** @return true if right trigger is pressed beyond threshold (default 0.5) */
    default boolean isRightTriggerPressed() { return getRightTrigger() > 0.5f; }

    // ===== Digital Buttons =====

    boolean isButtonAPressed();
    boolean isButtonBPressed();
    boolean isButtonXPressed();
    boolean isButtonYPressed();
    boolean isButtonBackPressed();
    boolean isButtonStartPressed();
    boolean isButtonGuidePressed();
    boolean isLeftBumperPressed();
    boolean isRightBumperPressed();
    boolean isLeftStickClicked();
    boolean isRightStickClicked();
    boolean isDpadUpPressed();
    boolean isDpadDownPressed();
    boolean isDpadLeftPressed();
    boolean isDpadRightPressed();
    boolean isPaddle1Pressed();
    boolean isPaddle2Pressed();
    boolean isPaddle3Pressed();
    boolean isPaddle4Pressed();
    boolean isTouchpadPressed();
    boolean isShiftLayerActive();

    // ===== String-based Queries =====

    /**
     * Query a button state by name string.
     * Prefer {@link ButtonName} constants over raw strings.
     *
     * @param buttonName the button identifier (camelCase, e.g. "buttonA", "leftBumper")
     * @return true if the button is pressed, false if unknown
     */
    boolean isButtonPressed(String buttonName);

    /**
     * Query an analog axis value by name string.
     * Prefer {@link ButtonName} constants (e.g. {@link ButtonName#AXIS_LEFT_X}).
     *
     * @param axisName the axis identifier ("leftStickX", "rightTrigger", etc.)
     * @return normalized axis value, or 0.0f if unknown
     */
    float getAxisValue(String axisName);
}
