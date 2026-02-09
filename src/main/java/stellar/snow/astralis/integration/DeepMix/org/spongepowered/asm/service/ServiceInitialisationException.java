package org.spongepowered.asm.service;

public class ServiceInitialisationException
extends RuntimeException {
    public ServiceInitialisationException() {
    }

    public ServiceInitialisationException(String message) {
        super(message);
    }

    public ServiceInitialisationException(Throwable cause) {
        super(cause);
    }

    public ServiceInitialisationException(String message, Throwable cause) {
        super(message, cause);
    }
}

