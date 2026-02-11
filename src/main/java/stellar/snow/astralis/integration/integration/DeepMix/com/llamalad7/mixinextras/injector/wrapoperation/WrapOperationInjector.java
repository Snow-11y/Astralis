package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.injector.IntLikeBehaviour;
import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import com.llamalad7.mixinextras.utils.OperationUtils;
import com.llamalad7.mixinextras.utils.PreviousInjectorInsns;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

class WrapOperationInjector
extends Injector {
    private final Type operationType;
    private final List<OperationConstructor> operationTypes;

    public WrapOperationInjector(InjectionInfo info) {
        super(info, "@WrapOperation");
        this.operationType = MixinExtrasService.getInstance().changePackage(Operation.class, Type.getType((String)CompatibilityHelper.getAnnotation((InjectionInfo)this.info).desc), WrapOperation.class);
        this.operationTypes = Arrays.asList((x$0, x$1, x$2) -> new DynamicInstanceofRedirectOperation(x$0, x$1, x$2), (x$0, x$1, x$2) -> new DupedFactoryRedirectOperation(x$0, x$1, x$2), this::newComparisonExpression, (x$0, x$1, x$2) -> new MethodCallOperation(x$0, x$1, x$2), FieldAccessOperation::new, InstanceofOperation::new, this::newInstantiationOperation, SimpleOperation::new);
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode initialNode) {
        InjectionNodes.InjectionNode node = InsnExpander.doExpansion(initialNode, target, this.info);
        this.checkTargetModifiers(target, false);
        StackExtension stack = new StackExtension(target);
        OperationType operation = this.operationTypes.stream().map(it -> it.make(target, node, stack)).filter(Objects::nonNull).filter(OperationType::validate).findFirst().orElseThrow(() -> CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s", this.annotationType, target, this)));
        this.wrapOperation(target, operation, stack);
    }

    private void wrapOperation(Target target, OperationType operation, StackExtension stack) {
        InsnList insns = new InsnList();
        InjectionNodes.InjectionNode node = operation.node;
        Type[] argTypes = this.getCurrentArgTypes(node);
        Type returnType = this.getReturnType(node);
        AbstractInsnNode champion = this.invokeHandler(target, operation, node, argTypes, returnType, insns, stack);
        operation.afterHandlerCall(insns, champion);
        AbstractInsnNode finalTarget = node.getCurrentTarget();
        target.wrapNode(finalTarget, champion, insns, new InsnList());
        node.decorate("mixinextras_wrappedOperation", true);
        target.insns.remove(finalTarget);
    }

    private AbstractInsnNode invokeHandler(Target target, OperationType operation, InjectionNodes.InjectionNode node, Type[] argTypes, Type returnType, InsnList insns, StackExtension stack) {
        boolean hasExtraThis;
        Injector.InjectorData handler = new Injector.InjectorData(target, "operation wrapper");
        boolean bl = hasExtraThis = node.isReplaced() && node.getCurrentTarget().getOpcode() != 184;
        if (hasExtraThis) {
            argTypes = ArrayUtils.remove(argTypes, 0);
        }
        Type[] originalArgs = this.getOriginalArgTypes(node);
        this.validateParams(handler, returnType, ArrayUtils.add(originalArgs, this.operationType));
        int[] argMap = this.storeArgs(target, argTypes, insns, 0);
        if (hasExtraThis) {
            insns.add((AbstractInsnNode)new InsnNode(87));
        }
        if (!this.isStatic) {
            insns.add((AbstractInsnNode)new VarInsnNode(25, 0));
        }
        this.pushArgs(this.methodArgs, insns, argMap, 0, originalArgs.length);
        if (hasExtraThis) {
            insns.add((AbstractInsnNode)new VarInsnNode(25, 0));
        }
        this.pushArgs(argTypes, insns, argMap, originalArgs.length, argMap.length);
        this.makeOperation(operation, originalArgs, returnType, insns, hasExtraThis, ArrayUtils.subarray(argTypes, originalArgs.length, argTypes.length));
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }
        stack.receiver(this.isStatic);
        stack.extra(1);
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);
        AbstractInsnNode result = super.invokeHandler(insns);
        InjectorUtils.coerceReturnType(handler, insns, returnType);
        return result;
    }

    private void makeOperation(OperationType operation, Type[] argTypes, Type returnType, InsnList insns, boolean hasExtraThis, Type[] trailingParams) {
        OperationUtils.makeOperation(argTypes, returnType, insns, hasExtraThis, trailingParams, this.classNode, this.operationType, operation.getName(), (paramArrayIndex, loadArgs) -> {
            InsnList copied = new InsnList();
            operation.copyNode(copied, paramArrayIndex, loadArgs, returnType);
            return copied;
        });
    }

    private Type getReturnType(InjectionNodes.InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        if (node.hasDecoration("mixinextras_simpleOperationReturnType")) {
            return (Type)node.getDecoration("mixinextras_simpleOperationReturnType");
        }
        if (originalTarget.getOpcode() == 193) {
            return Type.BOOLEAN_TYPE;
        }
        if (originalTarget instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode)originalTarget;
            if (methodInsnNode.name.equals("<init>")) {
                return Type.getObjectType((String)methodInsnNode.owner);
            }
            return Type.getReturnType((String)methodInsnNode.desc);
        }
        if (originalTarget instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode)originalTarget;
            if (fieldInsnNode.getOpcode() == 180 || fieldInsnNode.getOpcode() == 178) {
                return Type.getType((String)fieldInsnNode.desc);
            }
            return Type.VOID_TYPE;
        }
        if (originalTarget.getOpcode() == 187) {
            TypeInsnNode typeInsnNode = (TypeInsnNode)originalTarget;
            return Type.getObjectType((String)typeInsnNode.desc);
        }
        throw new UnsupportedOperationException();
    }

    private Type[] getOriginalArgTypes(InjectionNodes.InjectionNode node) {
        if (node.hasDecoration("mixinextras_newArgTypes")) {
            return (Type[])node.getDecoration("mixinextras_newArgTypes");
        }
        if (node.hasDecoration("mixinextras_simpleOperationArgs")) {
            return this.cleanIntLikeArgs((Type[])node.getDecoration("mixinextras_simpleOperationArgs"));
        }
        return this.getEffectiveArgTypes(node.getOriginalTarget());
    }

    private Type[] getCurrentArgTypes(InjectionNodes.InjectionNode node) {
        if (!node.isReplaced() && node.hasDecoration("mixinextras_simpleOperationArgs")) {
            return this.cleanIntLikeArgs((Type[])node.getDecoration("mixinextras_simpleOperationArgs"));
        }
        return this.getEffectiveArgTypes(node.getCurrentTarget());
    }

    private Type[] cleanIntLikeArgs(Type[] originalArgs) {
        return new IntLikeBehaviour.MatchArgType(0).transform(this.info, Type.getMethodType((Type)this.returnType, (Type[])originalArgs), Type.getMethodType((Type)this.returnType, (Type[])this.methodArgs)).getArgumentTypes();
    }

    private Type[] getEffectiveArgTypes(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode)node;
            Type[] args = Type.getArgumentTypes((String)methodInsnNode.desc);
            if (methodInsnNode.name.equals("<init>")) {
                return args;
            }
            switch (methodInsnNode.getOpcode()) {
                case 184: {
                    break;
                }
                case 183: {
                    args = ArrayUtils.add(args, 0, Type.getObjectType((String)this.classNode.name));
                    break;
                }
                default: {
                    args = ArrayUtils.add(args, 0, Type.getObjectType((String)methodInsnNode.owner));
                }
            }
            return args;
        }
        if (node instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode)node;
            switch (fieldInsnNode.getOpcode()) {
                case 180: {
                    return new Type[]{Type.getObjectType((String)fieldInsnNode.owner)};
                }
                case 181: {
                    return new Type[]{Type.getObjectType((String)fieldInsnNode.owner), Type.getType((String)fieldInsnNode.desc)};
                }
                case 178: {
                    return new Type[0];
                }
                case 179: {
                    return new Type[]{Type.getType((String)fieldInsnNode.desc)};
                }
            }
        }
        if (node.getOpcode() == 193) {
            return new Type[]{ASMUtils.OBJECT_TYPE};
        }
        throw new UnsupportedOperationException();
    }

    private OperationType newInstantiationOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
        AbstractInsnNode newNode = node.getCurrentTarget();
        if (newNode.getOpcode() != 187) {
            return null;
        }
        node.decorate("mixinextras_wrappedOperation", true);
        return new InstantiationOperation(target, target.addInjectionNode((AbstractInsnNode)ASMUtils.findInitNodeFor(target, (TypeInsnNode)newNode)), stack, node);
    }

    private OperationType newComparisonExpression(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
        ComparisonInfo comparison = (ComparisonInfo)InjectorUtils.getInjectorSpecificDecoration(node, this.info, "mixinextras_comparisonInfo");
        if (comparison == null) {
            return null;
        }
        boolean isWrapped = node.hasDecoration("mixinextras_wrappedOperation");
        return new ComparisonOperation(target, node, stack, isWrapped, comparison);
    }

    @FunctionalInterface
    private static interface OperationConstructor {
        public OperationType make(Target var1, InjectionNodes.InjectionNode var2, StackExtension var3);
    }

    private static abstract class OperationType {
        protected final Target target;
        protected final InjectionNodes.InjectionNode node;
        protected final AbstractInsnNode originalTarget;
        protected final AbstractInsnNode currentTarget;
        protected final StackExtension stack;

        OperationType(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
            this.target = target;
            this.node = node;
            this.originalTarget = node.getOriginalTarget();
            this.currentTarget = node.getCurrentTarget();
            this.stack = stack;
        }

        abstract boolean validate();

        abstract String getName();

        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs, Type returnType) {
            loadArgs.accept(insns);
            insns.add(this.currentTarget.clone(Collections.emptyMap()));
            AbstractInsnNode coerceCast = InjectorUtils.findCoerce(this.node, returnType);
            if (coerceCast != null) {
                this.target.insns.remove(coerceCast);
                insns.add(coerceCast);
            }
        }

        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
        }
    }

    private static class InstantiationOperation
    extends OperationType {
        private final InjectionNodes.InjectionNode newNode;
        private final AbstractInsnNode newInsn;
        private final boolean isDuped;

        InstantiationOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack, InjectionNodes.InjectionNode newNode) {
            super(target, node, stack);
            this.newNode = newNode;
            this.newInsn = newNode.getCurrentTarget();
            this.isDuped = InjectorUtils.isDupedNew(newNode);
        }

        @Override
        boolean validate() {
            return true;
        }

        @Override
        String getName() {
            return "new" + StringUtils.substringAfterLast(((MethodInsnNode)this.currentTarget).owner, "/");
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs, Type returnType) {
            insns.add((AbstractInsnNode)new TypeInsnNode(187, ((MethodInsnNode)this.currentTarget).owner));
            insns.add((AbstractInsnNode)new InsnNode(89));
            super.copyNode(insns, paramArrayIndex, loadArgs, returnType);
        }

        @Override
        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
            InsnNode newReplacement;
            if (this.isDuped) {
                newReplacement = new InsnNode(1);
                this.stack.extra(1);
                insns.add((AbstractInsnNode)new InsnNode(91));
                insns.add((AbstractInsnNode)new InsnNode(87));
                insns.add((AbstractInsnNode)new InsnNode(87));
                insns.add((AbstractInsnNode)new InsnNode(87));
            } else {
                newReplacement = new InsnNode(0);
                insns.add((AbstractInsnNode)new InsnNode(87));
            }
            this.newNode.replace(champion);
            this.target.insns.set(this.newInsn, (AbstractInsnNode)newReplacement);
        }
    }

    private class ComparisonOperation
    extends MethodCallOperation {
        private final boolean isWrapped;
        private final ComparisonInfo comparison;

        ComparisonOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack, boolean isWrapped, ComparisonInfo comparison) {
            super(target, node, stack);
            this.isWrapped = isWrapped;
            this.comparison = comparison;
        }

        @Override
        boolean validate() {
            super.validate();
            return this.comparison != null;
        }

        @Override
        String getName() {
            return this.isWrapped ? super.getName() : "comparison";
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs, Type returnType) {
            if (this.isWrapped) {
                super.copyNode(insns, paramArrayIndex, loadArgs, returnType);
                PreviousInjectorInsns.COMPARISON_WRAPPER.moveNodes(this.target.insns, insns, this.currentTarget);
                if (!this.comparison.jumpOnTrue) {
                    ASMUtils.ifElse(insns, 154, () -> insns.add((AbstractInsnNode)new InsnNode(4)), () -> insns.add((AbstractInsnNode)new InsnNode(3)));
                }
                return;
            }
            loadArgs.accept(insns);
            ASMUtils.ifElse(insns, this.comparison.copyJump(insns), () -> insns.add((AbstractInsnNode)new InsnNode(this.comparison.jumpOnTrue ? 3 : 4)), () -> insns.add((AbstractInsnNode)new InsnNode(this.comparison.jumpOnTrue ? 4 : 3)));
        }

        @Override
        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
            ASMUtils.ifElse(insns, 154, () -> insns.add((AbstractInsnNode)new InsnNode(this.comparison.jumpOnTrue ? 3 : 4)), () -> insns.add((AbstractInsnNode)new InsnNode(this.comparison.jumpOnTrue ? 4 : 3)));
            if (!this.isWrapped) {
                insns.add((AbstractInsnNode)new JumpInsnNode(154, this.comparison.getJumpTarget(this.target)));
                this.comparison.cleanup(this.target);
            }
        }
    }

    private class MethodCallOperation
    extends OperationType {
        MethodCallOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            if (this.currentTarget instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode)this.currentTarget;
                if (methodInsnNode.name.equals("<init>")) {
                    throw CompatibilityHelper.makeInvalidInjectionException(WrapOperationInjector.this.info, String.format("%s annotation is trying to target an <init> call in %s in %s! If this is an instantiation, target the NEW instead.", WrapOperationInjector.this.annotationType, this.target, WrapOperationInjector.this));
                }
                return true;
            }
            return false;
        }

        @Override
        String getName() {
            return ((MethodInsnNode)this.currentTarget).name;
        }
    }

    private class DupedFactoryRedirectOperation
    extends MethodCallOperation {
        DupedFactoryRedirectOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return super.validate() && InjectorUtils.isDupedFactoryRedirect(this.node);
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs, Type returnType) {
            super.copyNode(insns, paramArrayIndex, loadArgs, returnType);
            PreviousInjectorInsns.DUPED_FACTORY_REDIRECT.moveNodes(this.target.insns, insns, this.currentTarget);
        }
    }

    private class DynamicInstanceofRedirectOperation
    extends MethodCallOperation {
        DynamicInstanceofRedirectOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return super.validate() && InjectorUtils.isDynamicInstanceofRedirect(this.node);
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs, Type returnType) {
            super.copyNode(insns, paramArrayIndex, loadArgs, returnType);
            insns.add((AbstractInsnNode)new VarInsnNode(25, paramArrayIndex));
            insns.add((AbstractInsnNode)new InsnNode(3));
            insns.add((AbstractInsnNode)new InsnNode(50));
            insns.add((AbstractInsnNode)new InsnNode(95));
            PreviousInjectorInsns.DYNAMIC_INSTANCEOF_REDIRECT.moveNodes(this.target.insns, insns, this.currentTarget);
        }

        @Override
        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
            insns.add((AbstractInsnNode)new InsnNode(95));
            insns.add((AbstractInsnNode)new InsnNode(87));
        }
    }

    private static class SimpleOperation
    extends OperationType {
        SimpleOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return !this.node.isReplaced() && this.node.hasDecoration("mixinextras_simpleOperationArgs") && this.node.hasDecoration("mixinextras_simpleOperationReturnType");
        }

        @Override
        String getName() {
            return Bytecode.getOpcodeName(this.currentTarget).toLowerCase(Locale.ROOT);
        }
    }

    private static class InstanceofOperation
    extends OperationType {
        InstanceofOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return this.currentTarget.getOpcode() == 193;
        }

        @Override
        String getName() {
            return "instanceof" + StringUtils.substringAfterLast(((TypeInsnNode)this.currentTarget).desc, "/");
        }
    }

    private static class FieldAccessOperation
    extends OperationType {
        FieldAccessOperation(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return this.currentTarget instanceof FieldInsnNode;
        }

        @Override
        String getName() {
            return ((FieldInsnNode)this.currentTarget).name;
        }
    }
}

