package org.spongepowered.asm.mixin.transformer;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessor;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinProxy;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

class MixinCoprocessorAccessor
extends MixinCoprocessor {
    protected final String sessionId;
    private final Map<String, MixinInfo> accessorMixins = new HashMap<String, MixinInfo>();

    MixinCoprocessorAccessor(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onPrepare(MixinInfo mixin) {
        if (mixin.isAccessor()) {
            this.registerAccessor(mixin);
        }
    }

    void registerAccessor(MixinInfo mixin) {
        this.accessorMixins.put(mixin.getClassName(), mixin);
    }

    @Override
    MixinCoprocessor.ProcessResult process(String className, ClassNode classNode) {
        if (!MixinEnvironment.getCompatibilityLevel().supports(1) || !this.accessorMixins.containsKey(className)) {
            return MixinCoprocessor.ProcessResult.NONE;
        }
        MixinInfo mixin = this.accessorMixins.get(className);
        boolean transformed = false;
        MixinInfo.MixinClassNode mixinClassNode = mixin.getClassNode(0);
        ClassInfo targetClass = mixin.getTargets().get(0);
        if (!Bytecode.hasFlag(mixinClassNode, 1)) {
            Bytecode.setVisibility((ClassNode)mixinClassNode, Bytecode.Visibility.PUBLIC);
            transformed = true;
        }
        for (MixinInfo.MixinMethodNode methodNode : mixinClassNode.mixinMethods) {
            if (!Bytecode.hasFlag(methodNode, 8)) continue;
            AnnotationNode accessor = methodNode.getVisibleAnnotation(Accessor.class);
            AnnotationNode invoker = methodNode.getVisibleAnnotation(Invoker.class);
            if (accessor == null && invoker == null) continue;
            ClassInfo.Method method = this.getAccessorMethod(mixin, methodNode, targetClass);
            MixinCoprocessorAccessor.createProxy(methodNode, targetClass, method);
            Annotations.setVisible(methodNode, MixinProxy.class, "sessionId", this.sessionId);
            classNode.methods.add(methodNode);
            transformed = true;
        }
        if (!transformed) {
            return MixinCoprocessor.ProcessResult.NONE;
        }
        Bytecode.replace(mixinClassNode, classNode);
        return MixinCoprocessor.ProcessResult.PASSTHROUGH_TRANSFORMED;
    }

    private ClassInfo.Method getAccessorMethod(MixinInfo mixin, MethodNode methodNode, ClassInfo targetClass) throws MixinTransformerError {
        ClassInfo.Method method = mixin.getClassInfo().findMethod(methodNode, 10);
        if (!method.isConformed()) {
            String uniqueName = targetClass.getMethodMapper().getUniqueName(mixin, methodNode, this.sessionId, true);
            method.conform(uniqueName);
        }
        return method;
    }

    private static void createProxy(MethodNode methodNode, ClassInfo targetClass, ClassInfo.Method method) {
        methodNode.access |= 0x1000;
        methodNode.instructions.clear();
        Type[] args = Type.getArgumentTypes((String)methodNode.desc);
        Type returnType = Type.getReturnType((String)methodNode.desc);
        Bytecode.loadArgs(args, methodNode.instructions, 0);
        methodNode.instructions.add((AbstractInsnNode)new MethodInsnNode(184, targetClass.getName(), method.getName(), methodNode.desc, targetClass.isInterface()));
        methodNode.instructions.add((AbstractInsnNode)new InsnNode(returnType.getOpcode(172)));
        methodNode.maxStack = Bytecode.getFirstNonArgLocalIndex(args, false);
        methodNode.maxLocals = 0;
    }
}

