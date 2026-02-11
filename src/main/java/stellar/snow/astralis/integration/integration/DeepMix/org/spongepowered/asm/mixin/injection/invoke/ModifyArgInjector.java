package org.spongepowered.asm.mixin.injection.invoke;

import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.invoke.InvokeInjector;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.ArgOffsets;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;

public class ModifyArgInjector
extends InvokeInjector {
    private final int index;
    private final boolean singleArgMode;

    public ModifyArgInjector(InjectionInfo info, int index) {
        super(info, "@ModifyArg");
        this.index = index;
        this.singleArgMode = this.methodArgs.length == 1;
    }

    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        if (this.singleArgMode && !this.methodArgs[0].equals((Object)this.returnType)) {
            throw new InvalidInjectionException((ISelectorContext)this.info, "@ModifyArg return type on " + this + " must match the parameter type. ARG=" + this.methodArgs[0] + " RETURN=" + this.returnType);
        }
    }

    @Override
    protected void checkTarget(Target target) {
        if (!this.isStatic && target.isStatic) {
            throw new InvalidInjectionException((ISelectorContext)this.info, "non-static callback method " + this + " targets a static method which is not supported");
        }
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
        boolean nested = node.hasDecoration("argOffsets");
        Object[] originalArgs = offsets.apply(args);
        if (originalArgs.length == 0) {
            throw new InvalidInjectionException((ISelectorContext)this.info, "@ModifyArg injector " + this + " targets a method invocation " + ((MethodInsnNode)node.getOriginalTarget()).name + "()" + Type.getReturnType((String)methodNode.desc) + " with no arguments!");
        }
        int argIndex = offsets.getArgIndex(this.findArgIndex(target, (Type[])originalArgs));
        int baseIndex = offsets.getStartIndex();
        InsnList insns = new InsnList();
        Target.Extension extraLocals = target.extendLocals();
        if (this.singleArgMode) {
            this.injectSingleArgHandler(target, extraLocals, args, argIndex, insns, nested);
        } else {
            if (!Arrays.equals(originalArgs, this.methodArgs)) {
                throw new InvalidInjectionException((ISelectorContext)this.info, "@ModifyArg injector " + this + " targets a method with an invalid signature " + Bytecode.getDescriptor((Type[])originalArgs) + ", expected " + Bytecode.getDescriptor(this.methodArgs));
            }
            this.injectMultiArgHandler(target, extraLocals, args, baseIndex, argIndex, insns, nested);
        }
        target.insns.insertBefore((AbstractInsnNode)methodNode, insns);
        Target.Extension extraStack = target.extendStack();
        if (!this.isStatic) {
            extraStack.add();
        }
        extraStack.add(this.methodArgs);
        extraStack.apply();
        extraLocals.apply();
    }

    private void injectSingleArgHandler(Target target, Target.Extension extraLocals, Type[] args, int argIndex, InsnList insns, boolean nested) {
        int[] argMap = target.generateArgMap(args, argIndex, nested);
        this.storeArgs(target, args, insns, argMap, argIndex, args.length, null, null);
        this.invokeHandlerWithArgs(args, insns, argMap, argIndex, argIndex + 1);
        this.pushArgs(args, insns, argMap, argIndex + 1, args.length);
        extraLocals.add(argMap[argMap.length - 1] - target.getMaxLocals() + args[args.length - 1].getSize());
    }

    private void injectMultiArgHandler(Target target, Target.Extension extraLocals, Type[] args, int baseIndex, int argIndex, InsnList insns, boolean nested) {
        int[] argMap = target.generateArgMap(args, baseIndex, nested);
        int[] handlerArgMap = baseIndex == 0 ? argMap : Arrays.copyOfRange(argMap, baseIndex, baseIndex + this.methodArgs.length);
        this.storeArgs(target, args, insns, argMap, baseIndex, args.length, null, null);
        this.pushArgs(args, insns, argMap, baseIndex, argIndex);
        this.invokeHandlerWithArgs(this.methodArgs, insns, handlerArgMap, 0, this.methodArgs.length);
        this.pushArgs(args, insns, argMap, argIndex + 1, args.length);
        extraLocals.add(argMap[argMap.length - 1] - target.getMaxLocals() + args[args.length - 1].getSize());
    }

    protected int findArgIndex(Target target, Type[] args) {
        if (this.index > -1) {
            if (this.index >= args.length || !args[this.index].equals((Object)this.returnType)) {
                throw new InvalidInjectionException((ISelectorContext)this.info, "Specified index " + this.index + " for @ModifyArg is invalid for args " + Bytecode.getDescriptor(args) + ", expected " + this.returnType + " on " + this);
            }
            return this.index;
        }
        int argIndex = -1;
        for (int arg = 0; arg < args.length; ++arg) {
            if (!args[arg].equals((Object)this.returnType)) continue;
            if (argIndex != -1) {
                throw new InvalidInjectionException((ISelectorContext)this.info, "Found duplicate args with index [" + argIndex + ", " + arg + "] matching type " + this.returnType + " for @ModifyArg target " + target + " in " + this + ". Please specify index of desired arg.");
            }
            argIndex = arg;
        }
        if (argIndex == -1) {
            throw new InvalidInjectionException((ISelectorContext)this.info, "Could not find arg matching type " + this.returnType + " for @ModifyArg target " + target + " in " + this);
        }
        return argIndex;
    }
}

