package org.spongepowered.asm.mixin.throwables;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;

public class MixinException
extends RuntimeException {
    private String activityDescriptor;

    public MixinException(String message) {
        super(message);
    }

    public MixinException(String message, IActivityContext context) {
        super(message);
        this.activityDescriptor = context != null ? context.toString() : null;
    }

    public MixinException(Throwable cause) {
        super(cause);
    }

    public MixinException(Throwable cause, IActivityContext context) {
        super(cause);
        this.activityDescriptor = context != null ? context.toString() : null;
    }

    public MixinException(String message, Throwable cause) {
        super(message, cause);
    }

    public MixinException(String message, Throwable cause, IActivityContext context) {
        super(message, cause);
        this.activityDescriptor = context != null ? context.toString() : null;
    }

    public void prepend(IActivityContext upstreamContext) {
        String strContext = upstreamContext.toString();
        this.activityDescriptor = this.activityDescriptor != null ? strContext + " -> " + this.activityDescriptor : " -> " + strContext;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return this.activityDescriptor != null ? message + " [" + this.activityDescriptor + "]" : message;
    }
}

