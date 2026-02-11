package org.spongepowered.asm.mixin.injection.modify;

import org.spongepowered.asm.mixin.throwables.MixinException;

public class InvalidImplicitDiscriminatorException
extends MixinException {
    public InvalidImplicitDiscriminatorException(String message) {
        super(message);
    }

    public InvalidImplicitDiscriminatorException(String message, Throwable cause) {
        super(message, cause);
    }
}

