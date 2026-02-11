package org.spongepowered.asm.mixin.injection.invoke;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.invoke.RedirectInjector;
import org.spongepowered.asm.mixin.injection.invoke.util.InsnFinder;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.SignaturePrinter;

public class ModifyConstantInjector
extends RedirectInjector {
    public ModifyConstantInjector(InjectionInfo info) {
        super(info, "@ModifyConstant");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        if (!this.preInject(node)) {
            return;
        }
        if (node.isReplaced()) {
            throw new UnsupportedOperationException("Target failure for " + this.info);
        }
        AbstractInsnNode targetNode = node.getCurrentTarget();
        if (targetNode instanceof TypeInsnNode) {
            this.checkTargetModifiers(target, false);
            this.injectTypeConstantModifier(target, (TypeInsnNode)targetNode);
            return;
        }
        if (targetNode instanceof JumpInsnNode) {
            this.checkTargetModifiers(target, false);
            this.injectExpandedConstantModifier(target, (JumpInsnNode)targetNode);
            return;
        }
        if (Bytecode.isConstant(targetNode)) {
            this.checkTargetModifiers(target, false);
            this.injectConstantModifier(target, targetNode);
            return;
        }
        throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s annotation is targetting an invalid insn in %s in %s", this.annotationType, target, this));
    }

    private void injectTypeConstantModifier(Target target, TypeInsnNode typeNode) {
        int opcode = typeNode.getOpcode();
        if (opcode != 193) {
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s annotation does not support %s insn in %s in %s", this.annotationType, Bytecode.getOpcodeName(opcode), target, this));
        }
        this.injectAtInstanceOf(target, typeNode);
    }

    private void injectExpandedConstantModifier(Target target, JumpInsnNode jumpNode) {
        int opcode = jumpNode.getOpcode();
        if (opcode < 155 || opcode > 158) {
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s annotation selected an invalid opcode %s in %s in %s", this.annotationType, Bytecode.getOpcodeName(opcode), target, this));
        }
        Target.Extension extraStack = target.extendStack();
        InsnList insns = new InsnList();
        insns.add((AbstractInsnNode)new InsnNode(3));
        AbstractInsnNode invoke = this.invokeConstantHandler(Type.getType((String)"I"), target, extraStack, insns, insns);
        insns.add((AbstractInsnNode)new JumpInsnNode(opcode + 6, jumpNode.label));
        extraStack.add(1).apply();
        target.replaceNode((AbstractInsnNode)jumpNode, invoke, insns);
    }

    private void injectConstantModifier(Target target, AbstractInsnNode constNode) {
        Type constantType = Bytecode.getConstantType(constNode);
        if (constantType.getSort() <= 5 && this.info.getMixin().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
            this.checkNarrowing(target, constNode, constantType);
        }
        Target.Extension extraStack = target.extendStack();
        InsnList before = new InsnList();
        InsnList after = new InsnList();
        AbstractInsnNode invoke = this.invokeConstantHandler(constantType, target, extraStack, before, after);
        extraStack.apply();
        target.wrapNode(constNode, invoke, before, after);
    }

    private AbstractInsnNode invokeConstantHandler(Type constantType, Target target, Target.Extension extraStack, InsnList before, InsnList after) {
        Injector.InjectorData handler = new Injector.InjectorData(target, "constant modifier");
        this.validateParams(handler, constantType, constantType);
        if (!this.isStatic) {
            before.insert((AbstractInsnNode)new VarInsnNode(25, 0));
            extraStack.add();
        }
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, after, target.getArgIndices(), 0, handler.captureTargetArgs, extraStack);
        }
        return this.invokeHandler(after);
    }

    private void checkNarrowing(Target target, AbstractInsnNode constNode, Type constantType) {
        int var;
        LocalVariableNode localVar;
        AbstractInsnNode pop = new InsnFinder().findPopInsn(target, constNode);
        if (pop == null) {
            return;
        }
        if (pop instanceof FieldInsnNode) {
            FieldInsnNode fieldNode = (FieldInsnNode)pop;
            Type fieldType = Type.getType((String)fieldNode.desc);
            this.checkNarrowing(target, constNode, constantType, fieldType, target.indexOf(pop), String.format("%s %s %s.%s", Bytecode.getOpcodeName(pop), SignaturePrinter.getTypeName(fieldType, false), fieldNode.owner.replace('/', '.'), fieldNode.name));
        } else if (pop.getOpcode() == 172) {
            this.checkNarrowing(target, constNode, constantType, target.returnType, target.indexOf(pop), "RETURN " + SignaturePrinter.getTypeName(target.returnType, false));
        } else if (pop.getOpcode() == 54 && (localVar = Locals.getLocalVariableAt(target.classNode, target.method, pop, var = ((VarInsnNode)pop).var)) != null && localVar.desc != null) {
            String name = localVar.name != null ? localVar.name : "unnamed";
            Type localType = Type.getType((String)localVar.desc);
            this.checkNarrowing(target, constNode, constantType, localType, target.indexOf(pop), String.format("ISTORE[var=%d] %s %s", var, SignaturePrinter.getTypeName(localType, false), name));
        }
    }

    private void checkNarrowing(Target target, AbstractInsnNode constNode, Type constantType, Type type, int index, String description) {
        int fromSort = constantType.getSort();
        int toSort = type.getSort();
        if (toSort < fromSort) {
            String fromType = SignaturePrinter.getTypeName(constantType, false);
            String toType = SignaturePrinter.getTypeName(type, false);
            String message = toSort == 1 ? ". Implicit conversion to <boolean> can cause nondeterministic (JVM-specific) behaviour!" : "";
            Level level = toSort == 1 ? Level.ERROR : Level.WARN;
            Injector.logger.log(level, "Narrowing conversion of <{}> to <{}> in {} target {} at opcode {} ({}){}", fromType, toType, this.info, target, index, description, message);
        }
    }
}

