package org.spongepowered.asm.mixin.injection.throwables;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

public class InvalidInjectionException
extends InvalidMixinException {
    private final ISelectorContext selectorContext;

    public InvalidInjectionException(IMixinContext context, String message) {
        super(context, message);
        this.selectorContext = null;
    }

    public InvalidInjectionException(IMixinContext context, String message, IActivityContext activityContext) {
        super(context, message, activityContext);
        this.selectorContext = null;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, String message) {
        super(selectorContext.getMixin(), message);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, String message, IActivityContext activityContext) {
        super(selectorContext.getMixin(), message, activityContext);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(IMixinContext context, Throwable cause) {
        super(context, cause);
        this.selectorContext = null;
    }

    public InvalidInjectionException(IMixinContext context, Throwable cause, IActivityContext activityContext) {
        super(context, cause, activityContext);
        this.selectorContext = null;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, Throwable cause) {
        super(selectorContext.getMixin(), cause);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, Throwable cause, IActivityContext activityContext) {
        super(selectorContext.getMixin(), cause, activityContext);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(IMixinContext context, String message, Throwable cause) {
        super(context, message, cause);
        this.selectorContext = null;
    }

    public InvalidInjectionException(IMixinContext context, String message, Throwable cause, IActivityContext activityContext) {
        super(context, message, cause, activityContext);
        this.selectorContext = null;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, String message, Throwable cause) {
        super(selectorContext.getMixin(), message, cause);
        this.selectorContext = selectorContext;
    }

    public InvalidInjectionException(ISelectorContext selectorContext, String message, Throwable cause, IActivityContext activityContext) {
        super(selectorContext.getMixin(), message, cause, activityContext);
        this.selectorContext = selectorContext;
    }

    public ISelectorContext getContext() {
        return this.selectorContext;
    }
}

