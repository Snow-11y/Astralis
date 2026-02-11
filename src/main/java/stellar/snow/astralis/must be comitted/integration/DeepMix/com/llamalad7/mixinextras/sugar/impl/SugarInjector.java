package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.impl.SugarApplicationException;
import com.llamalad7.mixinextras.sugar.impl.SugarApplicator;
import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import com.llamalad7.mixinextras.sugar.impl.SugarWrapper;
import com.llamalad7.mixinextras.sugar.impl.handlers.HandlerInfo;
import com.llamalad7.mixinextras.sugar.impl.handlers.HandlerTransformer;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.MixinInternals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

class SugarInjector {
    private final InjectionInfo injectionInfo;
    private final IMixinInfo mixin;
    private final MethodNode handler;
    private final List<AnnotationNode> sugarAnnotations;
    private final List<Type> parameterGenerics;
    private Map<Target, List<InjectionNodes.InjectionNode>> targets;
    private final List<SugarParameter> strippedSugars = new ArrayList<SugarParameter>();
    private final List<SugarApplicator> applicators = new ArrayList<SugarApplicator>();
    private final List<SugarApplicationException> exceptions = new ArrayList<SugarApplicationException>();

    SugarInjector(InjectionInfo injectionInfo, IMixinInfo mixin, MethodNode handler, List<AnnotationNode> sugarAnnotations, List<Type> parameterGenerics) {
        this.injectionInfo = injectionInfo;
        this.mixin = mixin;
        this.handler = handler;
        this.sugarAnnotations = sugarAnnotations;
        this.parameterGenerics = parameterGenerics;
    }

    void setTargets(Map<Target, List<InjectionNodes.InjectionNode>> targets) {
        this.targets = targets;
    }

    static void prepareMixin(IMixinInfo mixinInfo, ClassNode mixinNode) {
        for (MethodNode method : mixinNode.methods) {
            if (!SugarInjector.hasSugar(method)) continue;
            SugarInjector.wrapInjectorAnnotation(mixinInfo, method);
        }
    }

    static HandlerInfo getHandlerInfo(IMixinInfo mixin, MethodNode handler, List<AnnotationNode> sugarAnnotations, List<Type> generics) {
        ArrayList<HandlerTransformer> transformers = new ArrayList<HandlerTransformer>();
        for (SugarParameter sugar : SugarInjector.findSugars(handler, sugarAnnotations, generics)) {
            HandlerTransformer transformer = HandlerTransformer.create(mixin, sugar);
            if (transformer == null || !transformer.isRequired(handler)) continue;
            transformers.add(transformer);
        }
        if (transformers.isEmpty()) {
            return null;
        }
        HandlerInfo handlerInfo = new HandlerInfo();
        for (HandlerTransformer transformer : transformers) {
            transformer.transform(handlerInfo);
        }
        return handlerInfo;
    }

    private static boolean hasSugar(MethodNode method) {
        List[] annotations = method.invisibleParameterAnnotations;
        if (annotations == null) {
            return false;
        }
        for (List paramAnnotations : annotations) {
            if (!SugarInjector.isSugar(paramAnnotations)) continue;
            return true;
        }
        return false;
    }

    private static boolean isSugar(List<AnnotationNode> paramAnnotations) {
        if (paramAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : paramAnnotations) {
            if (!SugarApplicator.isSugar(annotation.desc)) continue;
            return true;
        }
        return false;
    }

    private static void wrapInjectorAnnotation(IMixinInfo mixin, MethodNode method) {
        AnnotationNode injectorAnnotation = InjectionInfo.getInjectorAnnotation(mixin, method);
        if (injectorAnnotation == null) {
            return;
        }
        List<AnnotationNode> sugars = SugarInjector.stripSugarAnnotations(method);
        Type annotationType = Type.getType((String)injectorAnnotation.desc);
        if (MixinExtrasService.getInstance().isClassOwned(annotationType.getClassName()) && annotationType.getInternalName().endsWith("WrapMethod")) {
            injectorAnnotation.visit("sugars", sugars);
            return;
        }
        AnnotationNode wrapped = new AnnotationNode(Type.getDescriptor(SugarWrapper.class));
        wrapped.visit("original", (Object)injectorAnnotation);
        wrapped.visit("signature", (Object)(method.signature == null ? "" : method.signature));
        wrapped.visit("sugars", sugars);
        method.visibleAnnotations.remove(injectorAnnotation);
        method.visibleAnnotations.add(wrapped);
    }

    private static List<AnnotationNode> stripSugarAnnotations(MethodNode method) {
        ArrayList<AnnotationNode> result = new ArrayList<AnnotationNode>();
        for (List annotations : method.invisibleParameterAnnotations) {
            AnnotationNode sugar = SugarInjector.findSugar(annotations);
            if (sugar != null) {
                result.add(sugar);
                annotations.remove(sugar);
                continue;
            }
            result.add(new AnnotationNode(Type.getDescriptor(Deprecated.class)));
        }
        return result;
    }

    void stripSugar() {
        this.strippedSugars.addAll(SugarInjector.findSugars(this.handler, this.sugarAnnotations, this.parameterGenerics));
        ArrayList<Type> params = new ArrayList<Type>();
        boolean foundSugar = false;
        int i = 0;
        for (Type type : Type.getArgumentTypes((String)this.handler.desc)) {
            if (!SugarApplicator.isSugar(this.sugarAnnotations.get((int)i).desc)) {
                if (foundSugar) {
                    throw new IllegalStateException(String.format("Found non-trailing sugared parameters on %s", this.handler.name + this.handler.desc));
                }
                params.add(type);
            } else {
                foundSugar = true;
            }
            ++i;
        }
        this.handler.desc = Type.getMethodDescriptor((Type)Type.getReturnType((String)this.handler.desc), (Type[])params.toArray(new Type[0]));
    }

    void prepareSugar() {
        this.makeApplicators();
        this.validateApplicators();
        this.prepareApplicators();
    }

    private void makeApplicators() {
        for (SugarParameter sugar : this.strippedSugars) {
            SugarApplicator applicator = SugarApplicator.create(this.injectionInfo, sugar);
            this.applicators.add(applicator);
        }
    }

    private void validateApplicators() {
        for (SugarApplicator applicator : this.applicators) {
            for (Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : this.targets.entrySet()) {
                Target target = entry.getKey();
                ListIterator<InjectionNodes.InjectionNode> it = entry.getValue().listIterator();
                while (it.hasNext()) {
                    InjectionNodes.InjectionNode node = it.next();
                    try {
                        applicator.validate(target, node);
                    }
                    catch (SugarApplicationException e) {
                        this.exceptions.add(new SugarApplicationException(String.format("Failed to validate sugar %s %s on method %s from mixin %s in target method %s at instruction %s", ASMUtils.annotationToString(applicator.sugar), ASMUtils.typeToString(applicator.paramType), this.handler, this.mixin, target, node), e));
                        it.remove();
                    }
                }
            }
        }
    }

    private void prepareApplicators() {
        for (Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : this.targets.entrySet()) {
            Target target = entry.getKey();
            for (InjectionNodes.InjectionNode node : entry.getValue()) {
                try {
                    for (SugarApplicator applicator : this.applicators) {
                        applicator.prepare(target, node);
                    }
                }
                catch (Exception e) {
                    throw new SugarApplicationException(String.format("Failed to prepare sugar for method %s from mixin %s in target method %s at instruction %s", this.handler, this.mixin, target, node), e);
                }
            }
        }
    }

    List<SugarApplicationException> getExceptions() {
        return this.exceptions;
    }

    void reSugarHandler() {
        ArrayList<Type> paramTypes = new ArrayList<Type>(Arrays.asList(Type.getArgumentTypes((String)this.handler.desc)));
        for (SugarParameter parameter : this.strippedSugars) {
            paramTypes.add(parameter.type);
        }
        this.handler.desc = Type.getMethodDescriptor((Type)Type.getReturnType((String)this.handler.desc), (Type[])paramTypes.toArray(new Type[0]));
    }

    void transformHandlerCalls(Map<Target, List<Pair<InjectionNodes.InjectionNode, MethodInsnNode>>> calls) {
        for (Map.Entry<Target, List<Pair<InjectionNodes.InjectionNode, MethodInsnNode>>> entry : calls.entrySet()) {
            Target target = entry.getKey();
            StackExtension stack = new StackExtension(target);
            for (Pair<InjectionNodes.InjectionNode, MethodInsnNode> pair : entry.getValue()) {
                InjectionNodes.InjectionNode sourceNode = pair.getLeft();
                MethodInsnNode handlerCall = pair.getRight();
                InjectionNodes.InjectionNode node = target.addInjectionNode((AbstractInsnNode)handlerCall);
                Map<String, Object> decorations = MixinInternals.getDecorations(sourceNode);
                for (Map.Entry<String, Object> decoration : decorations.entrySet()) {
                    if (!decoration.getKey().startsWith("mixinextras_persistent_")) continue;
                    node.decorate(decoration.getKey(), decoration.getValue());
                }
                try {
                    for (SugarApplicator applicator : this.applicators) {
                        applicator.inject(target, node, stack);
                    }
                }
                catch (Exception e) {
                    throw new SugarApplicationException(String.format("Failed to apply sugar to method %s from mixin %s in target method %s at instruction %s", this.handler, this.mixin, target, node), e);
                }
                handlerCall.desc = this.handler.desc;
            }
        }
    }

    private static List<SugarParameter> findSugars(MethodNode handler, List<AnnotationNode> sugarAnnotations, List<Type> generics) {
        ArrayList<SugarParameter> result = new ArrayList<SugarParameter>();
        Type[] paramTypes = Type.getArgumentTypes((String)handler.desc);
        int i = 0;
        int index = Bytecode.isStatic(handler) ? 0 : 1;
        for (Type paramType : paramTypes) {
            AnnotationNode sugar = sugarAnnotations.get(i);
            if (SugarApplicator.isSugar(sugar.desc)) {
                result.add(new SugarParameter(sugar, paramType, generics.get(i), index, i));
            }
            ++i;
            index += paramType.getSize();
        }
        return result;
    }

    private static AnnotationNode findSugar(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return null;
        }
        AnnotationNode result = null;
        for (AnnotationNode annotation : annotations) {
            if (!SugarApplicator.isSugar(annotation.desc)) continue;
            if (result != null) {
                throw new IllegalStateException("Found multiple sugars on the same parameter! Got " + annotations.stream().map(ASMUtils::annotationToString).collect(Collectors.joining(" ")));
            }
            result = annotation;
        }
        return result;
    }

    private static List<AnnotationNode> getParamAnnotations(MethodNode handler, int paramIndex) {
        List[] invisible = handler.invisibleParameterAnnotations;
        if (invisible != null && invisible.length >= paramIndex) {
            return invisible[paramIndex];
        }
        return null;
    }
}

