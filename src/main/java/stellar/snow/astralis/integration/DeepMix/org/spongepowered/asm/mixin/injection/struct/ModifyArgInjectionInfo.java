package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.ModifyArgInjector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

@InjectionInfo.AnnotationType(value=ModifyArg.class)
@InjectionInfo.HandlerPrefix(value="modify")
@InjectionInfo.InjectorOrder(value=1000)
public class ModifyArgInjectionInfo
extends InjectionInfo {
    public ModifyArgInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        int index = Annotations.getValue(injectAnnotation, "index", -1);
        return new ModifyArgInjector((InjectionInfo)this, index);
    }

    @Override
    protected String getDescription() {
        return "Argument modifier method";
    }
}

