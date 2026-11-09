package com.ifels.controlflex.api;

/**
 * One activation of a registered action.
 *
 * @since 0.9.2.2
 */
public interface ActionTrigger {

    String actionId();

    /** The mode the player bound this action to; the same action may arrive in different modes. */
    TriggerMode mode();

    /** Which phase produced the trigger. */
    ActionLayer layer();

    /** The screen / overlay class that matched, or {@code null} for {@link ActionLayer#IN_GAME}. */
    String targetClassName();
}
