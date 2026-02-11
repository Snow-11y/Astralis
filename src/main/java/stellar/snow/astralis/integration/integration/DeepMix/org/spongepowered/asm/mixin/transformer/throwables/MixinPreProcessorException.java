package org.spongepowered.asm.mixin.transformer.throwables;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.throwables.MixinException;

public class MixinPreProcessorException
extends MixinException {
    public MixinPreProcessorException(String message, IActivityContext context) {
        super(message, context);
    }

    public MixinPreProcessorException(String message, Throwable cause, IActivityContext context) {
        super(message, cause, context);
    }
}

