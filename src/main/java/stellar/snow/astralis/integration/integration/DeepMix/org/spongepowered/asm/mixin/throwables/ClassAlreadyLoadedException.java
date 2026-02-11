package org.spongepowered.asm.mixin.throwables;

import org.spongepowered.asm.mixin.throwables.MixinException;

public class ClassAlreadyLoadedException
extends MixinException {
    public ClassAlreadyLoadedException(String message) {
        super(message);
    }

    public ClassAlreadyLoadedException(Throwable cause) {
        super(cause);
    }

    public ClassAlreadyLoadedException(String message, Throwable cause) {
        super(message, cause);
    }
}

