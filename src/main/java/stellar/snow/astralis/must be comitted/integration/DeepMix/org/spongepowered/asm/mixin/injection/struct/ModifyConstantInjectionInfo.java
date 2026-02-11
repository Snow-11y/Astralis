package org.spongepowered.asm.mixin.injection.struct;

import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.ModifyConstantInjector;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.include.com.google.common.base.Strings;

@InjectionInfo.AnnotationType(value=ModifyConstant.class)
@InjectionInfo.HandlerPrefix(value="constant")
@InjectionInfo.InjectorOrder(value=10000)
public class ModifyConstantInjectionInfo
extends InjectionInfo {
    private static final String CONSTANT_ANNOTATION_CLASS = Constant.class.getName().replace('.', '/');

    public ModifyConstantInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation, "constant");
    }

    @Override
    protected void readInjectionPoints() {
        super.readInjectionPoints();
        if (this.injectionPointAnnotations.isEmpty()) {
            AnnotationNode c = new AnnotationNode(CONSTANT_ANNOTATION_CLASS);
            c.visit("log", (Object)Boolean.TRUE);
            this.injectionPointAnnotations.add(c);
        }
    }

    @Override
    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        Type returnType = Type.getReturnType((String)this.method.desc);
        for (AnnotationNode at : ats) {
            this.injectionPoints.add(new BeforeConstant(this.getMixin(), at, returnType.getDescriptor()));
        }
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new ModifyConstantInjector(this);
    }

    @Override
    protected String getDescription() {
        return "Constant modifier method";
    }

    @Override
    public String getSliceId(String id) {
        return Strings.nullToEmpty(id);
    }
}

