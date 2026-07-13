package com.example;

import com.ifels.controlflex.api.IControlFlexPlugin;

/** Plugin from a non-ControlFlex/non-cfx package — used to test registration rejection. */
public class ForeignPlugin implements IControlFlexPlugin {
    @Override public String getModId() { return "foreign_mod"; }
}
