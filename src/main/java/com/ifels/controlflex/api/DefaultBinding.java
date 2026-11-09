package com.ifels.controlflex.api;

/**
 * A default binding declared at registration time.
 *
 * <p>Typed on purpose: the internal serialized form ({@code "leftStickUp|HOLD"}) is not part
 * of the API, so a change to that format cannot break bridges.</p>
 *
 * <p>v1 supports a single button. A default is only used while the user has not configured the
 * action for that layer or screen; writing an empty value means "unbound" and is respected.</p>
 *
 * @since 0.9.2.2
 */
public final class DefaultBinding {

    private final String button;
    private final TriggerMode mode;

    private DefaultBinding(String button, TriggerMode mode) {
        this.button = button;
        this.mode = mode;
    }

    /**
     * @param button one of the {@link ButtonName} constants
     * @param mode   when the action fires
     */
    public static DefaultBinding of(String button, TriggerMode mode) {
        return new DefaultBinding(button, mode == null ? TriggerMode.PRESS : mode);
    }

    public String button() {
        return button;
    }

    public TriggerMode mode() {
        return mode;
    }
}
