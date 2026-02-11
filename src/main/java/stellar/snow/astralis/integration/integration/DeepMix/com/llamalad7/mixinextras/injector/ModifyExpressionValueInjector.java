package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.ArrayCreationInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.injector.IntLikeBehaviour;
import com.llamalad7.mixinextras.injector.MixinExtrasHooks;
import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import com.llamalad7.mixinextras.utils.PreviousInjectorInsns;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

public class ModifyExpressionValueInjector
extends Injector {
    public ModifyExpressionValueInjector(InjectionInfo info) {
        super(info, "@ModifyExpressionValue");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        node = InsnExpander.doExpansion(node, target, this.info);
        this.checkTargetReturnsAValue(target, node);
        this.checkTargetModifiers(target, false);
        StackExtension stack = new StackExtension(target);
        Type valueType = this.getReturnType(node);
        valueType = this.cleanIntLikeType(valueType);
        AbstractInsnNode valueNode = this.getValueNode(node, valueType);
        boolean shouldPop = false;
        if (valueNode instanceof TypeInsnNode && valueNode.getOpcode() == 187) {
            if (!InjectorUtils.isDupedNew(node)) {
                target.insns.insert(valueNode, (AbstractInsnNode)new InsnNode(89));
                stack.extra(1);
                node.decorate("mixinextras_newIsDuped", true);
                shouldPop = true;
            }
            valueNode = ASMUtils.findInitNodeFor(target, (TypeInsnNode)valueNode);
        }
        TargetInfo info = new TargetInfo(target, node);
        this.injectValueModifier(target, valueNode, valueType, info, shouldPop, stack);
    }

    private void checkTargetReturnsAValue(Target target, InjectionNodes.InjectionNode node) {
        Type returnType = this.getReturnType(node);
        if (returnType == Type.VOID_TYPE) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an instruction with a return type of 'void' in %s in %s", this.annotationType, target, this));
        }
        if (returnType == null) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s", this.annotationType, target, this));
        }
    }

    private Type cleanIntLikeType(Type valueType) {
        Type expectedDesc = IntLikeBehaviour.MatchReturnType.INSTANCE.transform(this.info, Type.getMethodType((Type)valueType, (Type[])new Type[]{valueType}), Type.getMethodType((Type)this.returnType, (Type[])this.methodArgs));
        return expectedDesc.getReturnType();
    }

    private AbstractInsnNode getValueNode(InjectionNodes.InjectionNode target, Type expectedType) {
        AbstractInsnNode coerceCast = InjectorUtils.findCoerce(target, expectedType);
        return coerceCast != null ? coerceCast : target.getCurrentTarget();
    }

    private void injectValueModifier(Target target, AbstractInsnNode valueNode, Type valueType, TargetInfo info, boolean shouldPop, StackExtension stack) {
        InsnList after = new InsnList();
        info.invokeHandler(valueType, after, stack);
        if (shouldPop) {
            after.add((AbstractInsnNode)new InsnNode(87));
        }
        target.insns.insert(info.getInsertionPoint(valueNode), after);
    }

    private void invokeHandler(Type valueType, Target target, InsnList after, StackExtension stack) {
        Injector.InjectorData handler = new Injector.InjectorData(target, "expression value modifier");
        this.validateParams(handler, valueType, valueType);
        if (!this.isStatic) {
            after.add((AbstractInsnNode)new VarInsnNode(25, 0));
            if (valueType.getSize() == 2) {
                stack.extra(1);
                after.add((AbstractInsnNode)new InsnNode(91));
                after.add((AbstractInsnNode)new InsnNode(87));
            } else {
                after.add((AbstractInsnNode)new InsnNode(95));
            }
        }
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, after, target.getArgIndices(), 0, handler.captureTargetArgs);
        }
        stack.receiver(this.isStatic);
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);
        this.invokeHandler(after);
        InjectorUtils.coerceReturnType(handler, after, valueType);
    }

    private Type getReturnType(InjectionNodes.InjectionNode node) {
        if (InjectorUtils.hasInjectorSpecificDecoration(node, this.info, "mixinextras_isStringConcatExpression")) {
            return Type.getType(String.class);
        }
        if (node.hasDecoration("mixinextras_simpleExpressionType")) {
            return (Type)node.getDecoration("mixinextras_simpleExpressionType");
        }
        AbstractInsnNode original = node.getOriginalTarget();
        if (original instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode)original;
            return Type.getReturnType((String)methodInsnNode.desc);
        }
        if (original instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode)original;
            if (fieldInsnNode.getOpcode() == 180 || fieldInsnNode.getOpcode() == 178) {
                return Type.getType((String)fieldInsnNode.desc);
            }
            return Type.VOID_TYPE;
        }
        if (original.getOpcode() == 187 || original.getOpcode() == 192) {
            TypeInsnNode typeInsnNode = (TypeInsnNode)original;
            return Type.getObjectType((String)typeInsnNode.desc);
        }
        if (original.getOpcode() == 193) {
            return Type.BOOLEAN_TYPE;
        }
        Type constantType = ASMUtils.getConstantType(original);
        if (constantType != null) {
            return constantType;
        }
        return null;
    }

    private class TargetInfo {
        private final Target target;
        private final boolean isDupedFactoryRedirect;
        private final boolean isDynamicInstanceofRedirect;
        private final ArrayCreationInfo arrayCreationInfo;
        private final boolean isStringConcat;
        private final ComparisonInfo comparison;

        public TargetInfo(Target target, InjectionNodes.InjectionNode node) {
            this.target = target;
            this.isDupedFactoryRedirect = InjectorUtils.isDupedFactoryRedirect(node);
            this.isDynamicInstanceofRedirect = InjectorUtils.isDynamicInstanceofRedirect(node);
            this.arrayCreationInfo = (ArrayCreationInfo)node.getDecoration("mixinextras_persistent_arrayCreationInfo");
            this.isStringConcat = InjectorUtils.hasInjectorSpecificDecoration(node, ModifyExpressionValueInjector.this.info, "mixinextras_isStringConcatExpression");
            this.comparison = (ComparisonInfo)InjectorUtils.getInjectorSpecificDecoration(node, ModifyExpressionValueInjector.this.info, "mixinextras_comparisonInfo");
        }

        public AbstractInsnNode getInsertionPoint(AbstractInsnNode valueNode) {
            if (this.isDupedFactoryRedirect) {
                return PreviousInjectorInsns.DUPED_FACTORY_REDIRECT.getLast(valueNode);
            }
            if (this.isDynamicInstanceofRedirect) {
                return PreviousInjectorInsns.DYNAMIC_INSTANCEOF_REDIRECT.getLast(valueNode);
            }
            if (this.arrayCreationInfo != null) {
                return this.arrayCreationInfo.initialized.getNode(this.target).getCurrentTarget();
            }
            if (this.comparison != null) {
                return this.comparison.getJumpInsn(this.target);
            }
            return valueNode;
        }

        public void invokeHandler(Type valueType, InsnList after, StackExtension stack) {
            LabelNode originalJumpTarget = null;
            if (this.isStringConcat) {
                after.add((AbstractInsnNode)new InsnNode(89));
                stack.extra(1);
                after.add((AbstractInsnNode)new MethodInsnNode(182, Type.getInternalName(StringBuilder.class), "toString", Bytecode.generateDescriptor(String.class, new Object[0]), false));
            } else if (this.comparison != null) {
                originalJumpTarget = this.comparison.getJumpTarget(this.target);
                ASMUtils.ifElse(after, label -> {
                    this.comparison.getJumpInsn((Target)this.target).label = label;
                }, () -> after.add((AbstractInsnNode)new InsnNode(this.comparison.jumpOnTrue ? 3 : 4)), () -> after.add((AbstractInsnNode)new InsnNode(this.comparison.jumpOnTrue ? 4 : 3)));
            }
            ModifyExpressionValueInjector.this.invokeHandler(valueType, this.target, after, stack);
            if (this.isStringConcat) {
                after.add((AbstractInsnNode)new MethodInsnNode(184, Type.getInternalName(MixinExtrasHooks.class), "replaceContents", Bytecode.generateDescriptor(StringBuilder.class, StringBuilder.class, String.class), false));
            } else if (this.comparison != null) {
                after.add((AbstractInsnNode)new JumpInsnNode(this.comparison.jumpOnTrue ? 154 : 153, originalJumpTarget));
            }
        }
    }
}

