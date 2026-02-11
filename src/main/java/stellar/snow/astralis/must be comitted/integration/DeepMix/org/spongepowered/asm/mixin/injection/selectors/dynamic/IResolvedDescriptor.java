package org.spongepowered.asm.mixin.injection.selectors.dynamic;

import java.util.List;
import org.objectweb.asm.Type;
import org.spongepowered.asm.util.Quantifier;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

public interface IResolvedDescriptor {
    public boolean isResolved();

    public boolean isDebug();

    public IAnnotationHandle getAnnotation();

    public String getResolutionInfo();

    public String getId();

    public Type getOwner();

    public String getName();

    public Type[] getArgs();

    public Type getReturnType();

    public Quantifier getMatches();

    public List<IAnnotationHandle> getNext();
}

