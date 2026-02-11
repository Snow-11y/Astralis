package org.spongepowered.asm.mixin.throwables;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.throwables.MixinException;

public class ClassMetadataNotFoundException
extends MixinException {
    public ClassMetadataNotFoundException(String message) {
        super(message);
    }

    public ClassMetadataNotFoundException(String message, IActivityContext context) {
        super(message, context);
    }

    public ClassMetadataNotFoundException(Throwable cause) {
        super(cause);
    }

    public ClassMetadataNotFoundException(Throwable cause, IActivityContext context) {
        super(cause, context);
    }

    public ClassMetadataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClassMetadataNotFoundException(String message, Throwable cause, IActivityContext context) {
        super(message, cause, context);
    }
}

