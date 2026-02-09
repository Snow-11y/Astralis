package org.spongepowered.asm.mixin.injection.throwables;

import org.spongepowered.asm.mixin.throwables.MixinError;

public class InjectionError
extends MixinError {
    public InjectionError() {
    }

    public InjectionError(String message) {
        super(message);
    }

    public InjectionError(Throwable cause) {
        super(cause);
    }

    public InjectionError(String message, Throwable cause) {
        super(message, cause);
    }
}

