package com.ifels.controlflex.api;

/**
 * State handle for a registered action.
 *
 * <p>Read it on the client main thread. Do not cache the boolean across ticks: it changes with
 * the binding, the phase and the controller connection.</p>
 *
 * @since 0.9.2.2
 */
public interface IControlFlexAction {

    String getActionId();

    /**
     * Whether the action is active right now.
     *
     * <p>Hold-style modes ({@code HOLD}, {@code DELAY_HOLD}, {@code LONG_HOLD}, {@code
     * DOUBLE_HOLD}) are true while the combo is held; latching modes stay true until toggled
     * off; one-shot modes are true only on the tick they fired. An unbound or cleared action is
     * always false.</p>
     */
    boolean isActive();
}
