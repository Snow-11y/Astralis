package org.spongepowered.include.com.google.common.collect;

public abstract class ForwardingObject {
    protected ForwardingObject() {
    }

    protected abstract Object delegate();

    public String toString() {
        return this.delegate().toString();
    }
}

