package org.spongepowered.asm.mixin.injection.invoke;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.invoke.InvokeInjector;
import org.spongepowered.asm.mixin.injection.invoke.arg.ArgsClassGenerator;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.ArgOffsets;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;

public class ModifyArgsInjector
extends InvokeInjector {
    private final ArgsClassGenerator argsClassGenerator;

    public ModifyArgsInjector(InjectionInfo info) {
        super(info, "@ModifyArgs");
        this.argsClassGenerator = (ArgsClassGenerator)info.getMixin().getExtensions().getGenerator(ArgsClassGenerator.class);
    }

    @Override
    protected void checkTarget(Target target) {
        this.checkTargetModifiers(target, false);
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        this.checkTargetForNode(target, node, InjectionPoint.RestrictTargetLevel.ALLOW_ALL);
        super.inject(target, node);
    }

    @Override
    protected void injectAtInvoke(Target target, InjectionNodes.InjectionNode node) {
        MethodInsnNode methodNode = (MethodInsnNode)node.getCurrentTarget();
        Type[] args = Type.getArgumentTypes((String)methodNode.desc);
        ArgOffsets offsets = node.getDecoration("argOffsets", ArgOffsets.DEFAULT);
        Type[] originalArgs = offsets.apply(args);
        int endIndex = offsets.getArgIndex(originalArgs.length);
        String targetMethodDesc = Type.getMethodDescriptor((Type)Type.getReturnType((String)methodNode.desc), (Type[])originalArgs);
        if (originalArgs.length == 0) {
            throw new InvalidInjectionException((ISelectorContext)this.info, "@ModifyArgs injector " + this + " targets a method invocation " + ((MethodInsnNode)node.getOriginalTarget()).name + targetMethodDesc + " with no arguments!");
        }
        String clArgs = this.argsClassGenerator.getArgsClass(targetMethodDesc, this.info.getMixin().getMixin()).getName();
        boolean withArgs = this.verifyTarget(target);
        InsnList insns = new InsnList();
        Target.Extension extraStack = target.extendStack().add(1);
        int[] afterWindowArgMap = this.storeArgs(target, args, insns, endIndex);
        this.packArgs(insns, clArgs, targetMethodDesc);
        if (withArgs) {
            extraStack.add(target.arguments);
            Bytecode.loadArgs(target.arguments, insns, target.isStatic ? 0 : 1);
        }
        this.invokeHandler(insns);
        this.unpackArgs(insns, clArgs, originalArgs);
        this.pushArgs(args, insns, afterWindowArgMap, endIndex, args.length);
        extraStack.apply();
        target.insns.insertBefore((AbstractInsnNode)methodNode, insns);
    }

    private boolean verifyTarget(Target target) {
        String shortDesc = String.format("(L%s;)V", ArgsClassGenerator.ARGS_REF);
        if (!this.methodNode.desc.equals(shortDesc)) {
            String targetDesc = Bytecode.changeDescriptorReturnType(target.getDesc(), "V");
            String longDesc = String.format("(L%s;%s", ArgsClassGenerator.ARGS_REF, targetDesc.substring(1));
            if (this.methodNode.desc.equals(longDesc)) {
                return true;
            }
            throw new InvalidInjectionException((ISelectorContext)this.info, "@ModifyArgs injector " + this + " has an invalid signature " + this.methodNode.desc + ", expected " + shortDesc + " or " + longDesc);
        }
        return false;
    }

    private void packArgs(InsnList insns, String clArgs, String targetMethodDesc) {
        String factoryDesc = Bytecode.changeDescriptorReturnType(targetMethodDesc, "L" + clArgs + ";");
        insns.add((AbstractInsnNode)new MethodInsnNode(184, clArgs, "of", factoryDesc, false));
        insns.add((AbstractInsnNode)new InsnNode(89));
        if (!this.isStatic) {
            insns.add((AbstractInsnNode)new VarInsnNode(25, 0));
            insns.add((AbstractInsnNode)new InsnNode(95));
        }
    }

    private void unpackArgs(InsnList insns, String clArgs, Type[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (i < args.length - 1) {
                insns.add((AbstractInsnNode)new InsnNode(89));
            }
            insns.add((AbstractInsnNode)new MethodInsnNode(182, clArgs, "$" + i, "()" + args[i].getDescriptor(), false));
            if (i >= args.length - 1) continue;
            if (args[i].getSize() == 1) {
                insns.add((AbstractInsnNode)new InsnNode(95));
                continue;
            }
            insns.add((AbstractInsnNode)new InsnNode(93));
            insns.add((AbstractInsnNode)new InsnNode(88));
        }
    }
}

