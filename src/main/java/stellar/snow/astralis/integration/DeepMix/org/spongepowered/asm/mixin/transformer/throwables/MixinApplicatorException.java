package org.spongepowered.asm.mixin.transformer.throwables;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

public class MixinApplicatorException
extends InvalidMixinException {
    public MixinApplicatorException(IMixinInfo context, String message) {
        super(context, message, (IActivityContext)null);
    }

    public MixinApplicatorException(IMixinInfo context, String message, IActivityContext activityContext) {
        super(context, message, activityContext);
    }

    public MixinApplicatorException(IMixinContext context, String message) {
        super(context, message, (IActivityContext)null);
    }

    public MixinApplicatorException(IMixinContext context, String message, IActivityContext activityContext) {
        super(context, message, activityContext);
    }

    public MixinApplicatorException(IMixinInfo mixin, String message, Throwable cause) {
        super(mixin, message, cause, (IActivityContext)null);
    }

    public MixinApplicatorException(IMixinInfo mixin, String message, Throwable cause, IActivityContext activityContext) {
        super(mixin, message, cause, activityContext);
    }

    public MixinApplicatorException(IMixinContext mixin, String message, Throwable cause) {
        super(mixin, message, cause, (IActivityContext)null);
    }

    public MixinApplicatorException(IMixinContext mixin, String message, Throwable cause, IActivityContext activityContext) {
        super(mixin, message, cause, activityContext);
    }

    public MixinApplicatorException(IMixinInfo mixin, Throwable cause) {
        super(mixin, cause, (IActivityContext)null);
    }

    public MixinApplicatorException(IMixinInfo mixin, Throwable cause, IActivityContext activityContext) {
        super(mixin, cause, activityContext);
    }

    public MixinApplicatorException(IMixinContext mixin, Throwable cause) {
        super(mixin, cause, (IActivityContext)null);
    }

    public MixinApplicatorException(IMixinContext mixin, Throwable cause, IActivityContext activityContext) {
        super(mixin, cause, activityContext);
    }
}

