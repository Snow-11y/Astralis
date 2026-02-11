package org.spongepowered.asm.mixin.transformer.throwables;

import org.spongepowered.asm.mixin.throwables.MixinError;

public class MixinTransformerError
extends MixinError {
    public MixinTransformerError(String message) {
        super(message);
    }

    public MixinTransformerError(Throwable cause) {
        super(cause);
    }

    public MixinTransformerError(String message, Throwable cause) {
        super(message, cause);
    }
}

