package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.sugar.impl.SugarApplicationException;
import com.llamalad7.mixinextras.sugar.impl.SugarApplicator;
import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import com.llamalad7.mixinextras.sugar.impl.SugarPostProcessingExtension;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefClassGenerator;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import java.util.HashMap;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.modify.InvalidImplicitDiscriminatorException;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

class LocalSugarApplicator
extends SugarApplicator {
    private final boolean isArgsOnly;
    private final Type targetLocalType;
    private final boolean isMutable;

    LocalSugarApplicator(InjectionInfo info, SugarParameter parameter) {
        super(info, parameter);
        this.targetLocalType = LocalRefUtils.getTargetType(this.paramType, this.paramGeneric);
        this.isMutable = this.targetLocalType != this.paramType;
        this.isArgsOnly = Annotations.getValue(this.sugar, "argsOnly", Boolean.valueOf(false));
    }

    @Override
    void validate(Target target, InjectionNodes.InjectionNode node) {
        LocalVariableDiscriminator discriminator = LocalVariableDiscriminator.parse(this.sugar);
        LocalVariableDiscriminator.Context context = InjectorUtils.getOrCreateLocalContext(target, node, this.info, this.targetLocalType, this.isArgsOnly);
        if (discriminator.printLVT()) {
            InjectorUtils.printLocals(target, node.getCurrentTarget(), context, discriminator, this.targetLocalType, this.isArgsOnly);
            this.info.addCallbackInvocation(this.info.getMethod());
            throw new SugarApplicationException("Application aborted because locals are being printed instead.");
        }
        try {
            if (discriminator.findLocal(context) < 0) {
                throw new SugarApplicationException("Unable to find matching local!");
            }
        }
        catch (InvalidImplicitDiscriminatorException e) {
            throw new SugarApplicationException("Invalid implicit variable discriminator: ", e);
        }
    }

    @Override
    void prepare(Target target, InjectionNodes.InjectionNode node) {
        InjectorUtils.getOrCreateLocalContext(target, node, this.info, this.targetLocalType, this.isArgsOnly);
    }

    @Override
    void inject(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
        LocalVariableDiscriminator.Context context;
        LocalVariableDiscriminator discriminator = LocalVariableDiscriminator.parse(this.sugar);
        int index = discriminator.findLocal(context = InjectorUtils.getOrCreateLocalContext(target, node, this.info, this.targetLocalType, this.isArgsOnly));
        if (index < 0) {
            throw new SugarApplicationException("Failed to match a local, this should have been caught during validation.");
        }
        if (this.isMutable) {
            this.initAndLoadLocalRef(target, node, index, stack);
        } else {
            stack.extra(this.targetLocalType.getSize());
            target.insns.insertBefore(node.getCurrentTarget(), (AbstractInsnNode)new VarInsnNode(this.targetLocalType.getOpcode(21), index));
        }
    }

    @Override
    int postProcessingPriority() {
        return 1000;
    }

    private void initAndLoadLocalRef(Target target, InjectionNodes.InjectionNode node, int index, StackExtension stack) {
        String refName = LocalRefClassGenerator.getForType(this.targetLocalType);
        int refIndex = this.getOrCreateRef(target, node, index, refName, stack);
        stack.extra(1);
        target.insns.insertBefore(node.getCurrentTarget(), (AbstractInsnNode)new VarInsnNode(25, refIndex));
    }

    private int getOrCreateRef(Target target, InjectionNodes.InjectionNode node, int index, String refImpl, StackExtension stack) {
        HashMap<Integer, Integer> refIndices = (HashMap<Integer, Integer>)node.getDecoration("mixinextras_localRefMap");
        if (refIndices == null) {
            refIndices = new HashMap<Integer, Integer>();
            node.decorate("mixinextras_localRefMap", refIndices);
        }
        if (refIndices.containsKey(index)) {
            return (Integer)refIndices.get(index);
        }
        int refIndex = target.allocateLocal();
        target.addLocalVariable(refIndex, "ref" + refIndex, 'L' + refImpl + ';');
        InsnList construction = new InsnList();
        LocalRefUtils.generateNew(construction, this.targetLocalType);
        construction.add((AbstractInsnNode)new VarInsnNode(58, refIndex));
        target.insertBefore(node, construction);
        SugarPostProcessingExtension.enqueuePostProcessing(this, () -> {
            InsnList initialization = new InsnList();
            initialization.add((AbstractInsnNode)new VarInsnNode(25, refIndex));
            initialization.add((AbstractInsnNode)new VarInsnNode(this.targetLocalType.getOpcode(21), index));
            LocalRefUtils.generateInitialization(initialization, this.targetLocalType);
            target.insertBefore(node, initialization);
            InsnList after = new InsnList();
            after.add((AbstractInsnNode)new VarInsnNode(25, refIndex));
            LocalRefUtils.generateDisposal(after, this.targetLocalType);
            after.add((AbstractInsnNode)new VarInsnNode(this.targetLocalType.getOpcode(54), index));
            target.insns.insert(node.getCurrentTarget(), after);
        });
        stack.extra(this.targetLocalType.getSize() + 1);
        refIndices.put(index, refIndex);
        return refIndex;
    }
}

