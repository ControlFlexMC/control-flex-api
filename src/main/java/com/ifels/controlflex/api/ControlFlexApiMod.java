package com.ifels.controlflex.api;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric mod entry point.
 * No-op: this mod provides only API interfaces and the static hub class.
 * {@link ControlFlexApi} providers are injected by ControlFlex at its init time.
 */
public final class ControlFlexApiMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // No-op: API mod provides interfaces only.
    }
}
