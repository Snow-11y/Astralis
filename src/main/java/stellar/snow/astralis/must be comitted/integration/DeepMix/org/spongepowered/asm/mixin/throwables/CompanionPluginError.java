package org.spongepowered.asm.mixin.throwables;

public class CompanionPluginError
extends LinkageError {
    public CompanionPluginError() {
    }

    public CompanionPluginError(String message) {
        super(message);
    }

    public CompanionPluginError(String message, Throwable cause) {
        super(message, cause);
    }
}

