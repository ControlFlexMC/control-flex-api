package com.ifels.controlflex.api;

/**
 * Virtual gamepad input injector.
 *
 * <p>Lets bridge mods (e.g. test harnesses) inject synthetic controller
 * button/axis states that ControlFlex merges over the polled hardware state
 * on the client main thread. Injected input flows through the normal
 * binding pipeline (ComboManager, BindingMapper, cursor control, ...) exactly
 * like real controller input.</p>
 *
 * <p>Injected state is <b>sticky</b>: a button stays pressed until
 * {@link #pressButton(String, boolean)} is called with {@code false} (or
 * {@link #clearAll()} is invoked); an axis keeps its value until set to 0
 * (or cleared).</p>
 *
 * <p>Thread safety: implementations must be safe to call from any thread;
 * state is applied atomically on the client tick.</p>
 *
 * @since 0.8.6
 */
public interface IInputInjector {

    /**
     * Press or release a controller button by name.
     *
     * <p>Valid names follow {@link ButtonName} constants, e.g. {@code "buttonA"},
     * {@code "dpadUp"}, {@code "leftBumper"}. Direction names (e.g.
     * {@code "leftStickUp"}) are also accepted and map to the corresponding
     * stick axis. Unknown names are ignored.</p>
     *
     * @param buttonName button name (see {@link ButtonName})
     * @param pressed    true to press, false to release
     */
    void pressButton(String buttonName, boolean pressed);

    /**
     * Set an axis value by name, normalized to -1.0..1.0.
     *
     * <p>Valid names: {@code "leftStickX"}, {@code "leftStickY"},
     * {@code "rightStickX"}, {@code "rightStickY"}, {@code "leftTrigger"},
     * {@code "rightTrigger"}. Setting 0 removes the override.</p>
     *
     * @param axisName axis name
     * @param value    axis value in -1.0..1.0
     */
    void setAxis(String axisName, float value);

    /**
     * Release all injected buttons and zero all injected axes.
     */
    void clearAll();
}
