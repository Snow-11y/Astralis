package com.llamalad7.mixinextras.expression.impl.flow;

public class ComplexDataException
extends RuntimeException {
    public static final RuntimeException INSTANCE = new ComplexDataException();

    private ComplexDataException() {
        this.setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        this.setStackTrace(new StackTraceElement[0]);
        return this;
    }
}

