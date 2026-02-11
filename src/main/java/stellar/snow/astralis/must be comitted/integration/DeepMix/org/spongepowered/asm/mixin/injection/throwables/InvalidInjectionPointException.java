package org.spongepowered.asm.mixin.injection.throwables;

import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

public class InvalidInjectionPointException
extends InvalidInjectionException {
    public InvalidInjectionPointException(IMixinContext context, String format, Object ... args) {
        super(context, String.format(format, args));
    }

    public InvalidInjectionPointException(InjectionInfo info, String format, Object ... args) {
        super((ISelectorContext)info, String.format(format, args));
    }

    public InvalidInjectionPointException(IMixinContext context, Throwable cause, String format, Object ... args) {
        super(context, String.format(format, args), cause);
    }

    public InvalidInjectionPointException(InjectionInfo info, Throwable cause, String format, Object ... args) {
        super((ISelectorContext)info, String.format(format, args), cause);
    }
}

