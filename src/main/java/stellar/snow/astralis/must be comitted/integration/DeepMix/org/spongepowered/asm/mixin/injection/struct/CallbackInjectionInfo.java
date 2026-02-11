package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInjector;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.include.com.google.common.base.Strings;

@InjectionInfo.AnnotationType(value=Inject.class)
@InjectionInfo.InjectorOrder(value=1000)
public class CallbackInjectionInfo
extends InjectionInfo {
    protected CallbackInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        boolean cancellable = Annotations.getValue(injectAnnotation, "cancellable", Boolean.FALSE);
        LocalCapture locals = Annotations.getValue(injectAnnotation, "locals", LocalCapture.class, LocalCapture.NO_CAPTURE);
        String identifier = Annotations.getValue(injectAnnotation, "id", "");
        return new CallbackInjector(this, cancellable, locals, identifier);
    }

    @Override
    public String getSliceId(String id) {
        return Strings.nullToEmpty(id);
    }
}

