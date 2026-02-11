package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;

public interface ITargetSelectorConstructor
extends ITargetSelectorByName {
    public String toCtorType();

    public String toCtorDesc();
}

