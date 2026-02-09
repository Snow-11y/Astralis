package org.spongepowered.asm.mixin.throwables;

public class MixinError
extends Error {
    public MixinError() {
    }

    public MixinError(String message) {
        super(message);
    }

    public MixinError(Throwable cause) {
        super(cause);
    }

    public MixinError(String message, Throwable cause) {
        super(message, cause);
    }
}

