package org.spongepowered.asm.service;

import java.util.Collection;
import org.spongepowered.asm.service.ITransformer;

public interface ITransformerProvider {
    public Collection<ITransformer> getTransformers();

    public Collection<ITransformer> getDelegatedTransformers();

    public void addTransformerExclusion(String var1);
}

