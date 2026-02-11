package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;

public interface ITargetSelectorByName
extends ITargetSelector {
    public String getOwner();

    public String getName();

    public String getDesc();

    public String toDescriptor();

    public MatchResult matches(String var1, String var2, String var3);
}

