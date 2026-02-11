package org.spongepowered.asm.mixin.injection.throwables;

import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;

public class InjectionValidationException
extends Exception {
    private final InjectorGroupInfo group;

    public InjectionValidationException(InjectorGroupInfo group, String message) {
        super(message);
        this.group = group;
    }

    public InjectorGroupInfo getGroup() {
        return this.group;
    }
}

