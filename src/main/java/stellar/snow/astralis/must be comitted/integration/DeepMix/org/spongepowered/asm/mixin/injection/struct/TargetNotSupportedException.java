package org.spongepowered.asm.mixin.injection.struct;

import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;

public class TargetNotSupportedException
extends InvalidSelectorException {
    public TargetNotSupportedException(String message) {
        super(message);
    }

    public TargetNotSupportedException(Throwable cause) {
        super(cause);
    }

    public TargetNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}

