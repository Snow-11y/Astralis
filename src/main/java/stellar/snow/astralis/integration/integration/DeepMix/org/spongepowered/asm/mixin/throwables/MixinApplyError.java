package org.spongepowered.asm.mixin.throwables;

import org.spongepowered.asm.mixin.throwables.MixinError;

public class MixinApplyError
extends MixinError {
    public MixinApplyError(String message) {
        super(message);
    }

    public MixinApplyError(Throwable cause) {
        super(cause);
    }

    public MixinApplyError(String message, Throwable cause) {
        super(message, cause);
    }
}

