package org.spongepowered.asm.mixin.transformer;

import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.IMixinInternal;

public interface IMixinTransformerFactory
extends IMixinInternal {
    public IMixinTransformer createTransformer() throws MixinInitialisationError;
}

