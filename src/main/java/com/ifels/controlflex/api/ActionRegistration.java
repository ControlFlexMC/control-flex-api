package com.ifels.controlflex.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * One action a bridge mod declares to ControlFlex.
 *
 * <p>Build with {@link #builder(String)}. The action id is permanent: it is written into the
 * user's binding file, so changing it later loses their bindings.</p>
 *
 * @since 0.9.2.2
 */
public final class ActionRegistration {

    private final String actionId;
    private final String nameKey;
    private final String groupModId;
    private final Set<ActionLayer> layers;
    private final List<String> targetClasses;
    private final DefaultBinding defaultBinding;

    private ActionRegistration(String actionId, String nameKey, String groupModId,
                               Set<ActionLayer> layers, List<String> targetClasses,
                               DefaultBinding defaultBinding) {
        this.actionId = actionId;
        this.nameKey = nameKey;
        this.groupModId = groupModId;
        this.layers = Collections.unmodifiableSet(layers);
        this.targetClasses = Collections.unmodifiableList(targetClasses);
        this.defaultBinding = defaultBinding;
    }

    public static Builder builder(String actionId) {
        return new Builder(actionId);
    }

    public String actionId() {
        return actionId;
    }

    /** i18n key for the display name; falls back to {@link #actionId()} when empty. */
    public String nameKey() {
        return nameKey;
    }

    /** The mod whose adaptation card lists this action (for example {@code "cobblemon"}). */
    public String groupModId() {
        return groupModId;
    }

    /** Declared layers; empty means all three. */
    public Set<ActionLayer> layers() {
        return layers;
    }

    /** Screen / overlay class names this action is limited to; empty means every class. */
    public List<String> targetClasses() {
        return targetClasses;
    }

    /** Default binding, or {@code null} when the action starts unbound. */
    public DefaultBinding defaultBinding() {
        return defaultBinding;
    }

    public static final class Builder {

        private final String actionId;
        private String nameKey = "";
        private String groupModId = "";
        private final EnumSet<ActionLayer> layers = EnumSet.noneOf(ActionLayer.class);
        private final List<String> targetClasses = new ArrayList<String>();
        private DefaultBinding defaultBinding;

        private Builder(String actionId) {
            this.actionId = actionId;
        }

        public Builder nameKey(String nameKey) {
            this.nameKey = nameKey == null ? "" : nameKey;
            return this;
        }

        public Builder groupModId(String modId) {
            this.groupModId = modId == null ? "" : modId;
            return this;
        }

        /** Call once per layer; calling it not at all means all three. */
        public Builder layer(ActionLayer layer) {
            if (layer != null) {
                this.layers.add(layer);
            }
            return this;
        }

        /** Only meaningful for {@link ActionLayer#SCREEN} / {@link ActionLayer#OVERLAY}. */
        public Builder targetClass(String className) {
            if (className != null && !className.isEmpty()) {
                this.targetClasses.add(className);
            }
            return this;
        }

        public Builder defaultBinding(DefaultBinding binding) {
            this.defaultBinding = binding;
            return this;
        }

        public ActionRegistration build() {
            EnumSet<ActionLayer> resolved = layers.isEmpty()
                    ? EnumSet.allOf(ActionLayer.class)
                    : EnumSet.copyOf(layers);
            return new ActionRegistration(actionId, nameKey, groupModId, resolved,
                    new ArrayList<String>(targetClasses), defaultBinding);
        }
    }
}
