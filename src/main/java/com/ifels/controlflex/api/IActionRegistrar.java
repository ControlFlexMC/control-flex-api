package com.ifels.controlflex.api;

/**
 * Handed to {@link IControlFlexPlugin#onRegisterActions(IActionRegistrar)} for the duration of
 * that callback only.
 *
 * @since 0.9.2.2
 */
public interface IActionRegistrar {

    /**
     * Declare one action.
     *
     * <p>Invalid input is reported with a warning and yields a handle that is never active;
     * one bad registration does not abort the rest. Registering the same id twice returns the
     * existing handle; an id already owned by another plugin yields a never-active handle.</p>
     *
     * @param registration the action and its optional default binding
     * @return a handle for reading the action's state, never {@code null}
     * @throws IllegalStateException when called after the registration callback returned
     */
    IControlFlexAction register(ActionRegistration registration);
}
