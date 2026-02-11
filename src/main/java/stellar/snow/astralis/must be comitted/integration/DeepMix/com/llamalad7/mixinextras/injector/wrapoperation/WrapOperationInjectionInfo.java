package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.injector.MixinExtrasLateInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjector;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import com.llamalad7.mixinextras.utils.MixinExtrasLogger;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

@InjectionInfo.AnnotationType(value=WrapOperation.class)
@InjectionInfo.HandlerPrefix(value="wrapOperation")
public class WrapOperationInjectionInfo
extends MixinExtrasLateInjectionInfo {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("WrapOperation");

    public WrapOperationInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation, WrapOperationInjectionInfo.determineAtKey(mixin, method, annotation));
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapOperationInjector(this);
    }

    @Override
    public void prepare() {
        super.prepare();
        InjectorUtils.checkForDupedNews(this.targetNodes);
        for (Map.Entry entry : this.targetNodes.entrySet()) {
            Target target = (Target)entry.getKey();
            ListIterator it = ((List)entry.getValue()).listIterator();
            while (it.hasNext()) {
                InjectionNodes.InjectionNode node = (InjectionNodes.InjectionNode)it.next();
                AbstractInsnNode currentTarget = node.getCurrentTarget();
                if (currentTarget.getOpcode() != 187) continue;
                MethodInsnNode initCall = ASMUtils.findInitNodeFor(target, (TypeInsnNode)currentTarget);
                if (initCall == null) {
                    LOGGER.warn("NEW node {} in {} has no init call?", Bytecode.describeNode(currentTarget), target);
                    it.remove();
                    continue;
                }
                node.decorate("mixinextras_newArgTypes", Type.getArgumentTypes((String)initCall.desc));
            }
        }
    }

    @Override
    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        if (this.atKey.equals("at")) {
            super.parseInjectionPoints(ats);
            return;
        }
        Type returnType = Type.getReturnType((String)this.method.desc);
        for (AnnotationNode at : ats) {
            this.injectionPoints.add(new BeforeConstant(CompatibilityHelper.getMixin(this), at, returnType.getDescriptor()));
        }
    }

    @Override
    public String getLateInjectionType() {
        return "WrapOperation";
    }

    private static String determineAtKey(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        boolean constant;
        boolean at = Annotations.getValue(annotation, "at") != null;
        boolean bl = constant = Annotations.getValue(annotation, "constant") != null;
        if (at == constant) {
            throw new IllegalStateException(String.format("@WrapOperation injector %s::%s must specify exactly one of `at` and `constant`, got %s.", mixin.getMixin().getClassName(), method.name, at ? "both" : "neither"));
        }
        return at ? "at" : "constant";
    }
}

