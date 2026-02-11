package org.spongepowered.asm.mixin.transformer.throwables;

import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;

public class ReEntrantTransformerError
extends MixinTransformerError {
    public ReEntrantTransformerError(String message) {
        super(message);
    }

    public ReEntrantTransformerError(Throwable cause) {
        super(cause);
    }

    public ReEntrantTransformerError(String message, Throwable cause) {
        super(message, cause);
    }
}

