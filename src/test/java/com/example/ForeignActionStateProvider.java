package com.example;

import com.ifels.controlflex.api.IActionStateProvider;
import java.util.Set;

/** Stub from a non-ControlFlex package — used to test package validation rejection. */
public class ForeignActionStateProvider implements IActionStateProvider {
    @Override public boolean isGameActionActive(String actionId) { return false; }
    @Override public boolean isGuiActionActive(String actionId) { return false; }
    @Override public Set<String> getActiveGameActions() { return Set.of(); }
    @Override public Set<String> getActiveGuiActions() { return Set.of(); }
}
