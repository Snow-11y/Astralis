package org.spongepowered.asm.mixin.transformer.throwables;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.throwables.MixinException;

public class InvalidMixinException
extends MixinException {
    private final IMixinInfo mixin;

    public InvalidMixinException(IMixinInfo mixin, String message) {
        super(message);
        this.mixin = mixin;
    }

    public InvalidMixinException(IMixinInfo mixin, String message, IActivityContext activityContext) {
        super(message, activityContext);
        this.mixin = mixin;
    }

    public InvalidMixinException(IMixinContext context, String message) {
        this(context.getMixin(), message);
    }

    public InvalidMixinException(IMixinContext context, String message, IActivityContext activityContext) {
        this(context.getMixin(), message, activityContext);
    }

    public InvalidMixinException(IMixinInfo mixin, Throwable cause) {
        super(cause);
        this.mixin = mixin;
    }

    public InvalidMixinException(IMixinInfo mixin, Throwable cause, IActivityContext activityContext) {
        super(cause, activityContext);
        this.mixin = mixin;
    }

    public InvalidMixinException(IMixinContext context, Throwable cause) {
        this(context.getMixin(), cause);
    }

    public InvalidMixinException(IMixinContext context, Throwable cause, IActivityContext activityContext) {
        this(context.getMixin(), cause, activityContext);
    }

    public InvalidMixinException(IMixinInfo mixin, String message, Throwable cause) {
        super(message, cause);
        this.mixin = mixin;
    }

    public InvalidMixinException(IMixinInfo mixin, String message, Throwable cause, IActivityContext activityContext) {
        super(message, cause, activityContext);
        this.mixin = mixin;
    }

    public InvalidMixinException(IMixinContext context, String message, Throwable cause) {
        super(message, cause);
        this.mixin = context.getMixin();
    }

    public InvalidMixinException(IMixinContext context, String message, Throwable cause, IActivityContext activityContext) {
        super(message, cause, activityContext);
        this.mixin = context.getMixin();
    }

    public IMixinInfo getMixin() {
        return this.mixin;
    }
}

