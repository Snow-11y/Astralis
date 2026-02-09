package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

public class ATNDeserializationOptions {
    private static final ATNDeserializationOptions defaultOptions = new ATNDeserializationOptions();
    private boolean readOnly;
    private boolean verifyATN = true;
    private boolean generateRuleBypassTransitions = false;

    public static ATNDeserializationOptions getDefaultOptions() {
        return defaultOptions;
    }

    public final void makeReadOnly() {
        this.readOnly = true;
    }

    public final boolean isVerifyATN() {
        return this.verifyATN;
    }

    public final boolean isGenerateRuleBypassTransitions() {
        return this.generateRuleBypassTransitions;
    }

    static {
        defaultOptions.makeReadOnly();
    }
}

