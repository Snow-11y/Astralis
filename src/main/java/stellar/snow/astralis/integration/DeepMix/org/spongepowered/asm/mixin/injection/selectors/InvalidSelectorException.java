package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.mixin.throwables.MixinException;

public class InvalidSelectorException
extends MixinException {
    public InvalidSelectorException(String message) {
        super(message);
    }

    public InvalidSelectorException(Throwable cause) {
        super(cause);
    }

    public InvalidSelectorException(String message, Throwable cause) {
        super(message, cause);
    }
}

