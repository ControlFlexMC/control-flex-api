package com.ifels.controlflex.api;

import java.util.Objects;

/**
 * Interactive context declaration (spec 主 §10.2): className is required;
 * left/right null means "do not override that side".
 *
 * @since 0.8.6
 */
public record InteractiveContextHint(String className,
                                     StickBehavior leftStick,
                                     StickBehavior rightStick) {

    public InteractiveContextHint {
        Objects.requireNonNull(className, "className must be non-null (spec 主 §10.2)");
    }

    /** Static factory for future overloads (threshold/cursorRadius, spec 主 §10.2);
     *  keeps construction stable for bridge mods. */
    public static InteractiveContextHint of(String className, StickBehavior left, StickBehavior right) {
        return new InteractiveContextHint(className, left, right);
    }
}
