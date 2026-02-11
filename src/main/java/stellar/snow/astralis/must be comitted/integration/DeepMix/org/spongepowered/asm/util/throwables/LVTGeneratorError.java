package org.spongepowered.asm.util.throwables;

import org.spongepowered.asm.mixin.throwables.MixinError;

public class LVTGeneratorError
extends MixinError {
    public LVTGeneratorError(String message) {
        super(message);
    }

    public LVTGeneratorError(String message, Throwable cause) {
        super(message, cause);
    }
}

