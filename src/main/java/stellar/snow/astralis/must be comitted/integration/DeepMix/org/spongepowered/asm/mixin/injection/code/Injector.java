package org.spongepowered.asm.mixin.injection.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.include.com.google.common.collect.ObjectArrays;

public abstract class Injector {
    protected static final ILogger logger = MixinService.getService().getLogger("mixin");
    protected InjectionInfo info;
    protected final String annotationType;
    protected final ClassNode classNode;
    protected final MethodNode methodNode;
    protected final Type[] methodArgs;
    protected final Type returnType;
    protected final boolean isStatic;
    protected final boolean isInterface;

    public Injector(InjectionInfo info, String annotationType) {
        this.info = info;
        this.annotationType = annotationType;
        this.classNode = info.getTargetClassNode();
        this.methodNode = info.getMethod();
        this.methodArgs = Type.getArgumentTypes((String)this.methodNode.desc);
        this.returnType = Type.getReturnType((String)this.methodNode.desc);
        this.isStatic = Bytecode.isStatic(this.methodNode);
        this.isInterface = Bytecode.hasFlag(this.classNode, 512);
    }

    public String toString() {
        return String.format("%s::%s", this.classNode.name, this.info.getMethodName());
    }

    public final List<InjectionNodes.InjectionNode> find(InjectorTarget injectorTarget, List<InjectionPoint> injectionPoints) {
        this.sanityCheck(injectorTarget.getTarget(), injectionPoints);
        ArrayList<InjectionNodes.InjectionNode> myNodes = new ArrayList<InjectionNodes.InjectionNode>();
        for (TargetNode node : this.findTargetNodes(injectorTarget, injectionPoints)) {
            this.addTargetNode(injectorTarget, myNodes, node.insn, node.nominators);
        }
        return myNodes;
    }

    protected void addTargetNode(InjectorTarget injectorTarget, List<InjectionNodes.InjectionNode> myNodes, AbstractInsnNode node, Set<InjectionPoint> nominators) {
        myNodes.add(injectorTarget.addInjectionNode(node));
    }

    public final void preInject(Target target, List<InjectionNodes.InjectionNode> nodes) {
        for (InjectionNodes.InjectionNode node : nodes) {
            this.preInject(target, node);
        }
    }

    public final void inject(Target target, List<InjectionNodes.InjectionNode> nodes) {
        for (InjectionNodes.InjectionNode node : nodes) {
            if (node.isRemoved()) {
                if (!this.info.getMixin().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) continue;
                logger.warn("Target node for {} was removed by a previous injector in {}", this.info, target);
                continue;
            }
            this.inject(target, node);
        }
        for (InjectionNodes.InjectionNode node : nodes) {
            this.postInject(target, node);
        }
    }

    private Collection<TargetNode> findTargetNodes(InjectorTarget injectorTarget, List<InjectionPoint> injectionPoints) {
        IMixinContext mixin = this.info.getMixin();
        TreeMap<Integer, TargetNode> targetNodes = new TreeMap<Integer, TargetNode>();
        ArrayList<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>(32);
        for (InjectionPoint injectionPoint : injectionPoints) {
            nodes.clear();
            if (injectorTarget.isMerged() && !mixin.getClassName().equals(injectorTarget.getMergedBy()) && !injectionPoint.checkPriority(injectorTarget.getMergedPriority(), mixin.getPriority())) {
                throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s on %s with priority %d cannot inject into %s merged by %s with priority %d", injectionPoint, this, mixin.getPriority(), injectorTarget, injectorTarget.getMergedBy(), injectorTarget.getMergedPriority()));
            }
            if (!this.findTargetNodes(injectorTarget, injectionPoint, nodes)) continue;
            InjectionPoint.Specifier specifier = injectionPoint.getSpecifier(InjectionPoint.Specifier.ALL);
            if (specifier == InjectionPoint.Specifier.ONE && nodes.size() != 1) {
                throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s on %s has specifier :ONE but matched %d instructions", injectionPoint, this, nodes.size()));
            }
            if (specifier != InjectionPoint.Specifier.ALL && nodes.size() > 1) {
                AbstractInsnNode specified = (AbstractInsnNode)nodes.get(specifier == InjectionPoint.Specifier.FIRST ? 0 : nodes.size() - 1);
                this.addTargetNode(injectorTarget, targetNodes, injectionPoint, specified);
                continue;
            }
            for (AbstractInsnNode insn : nodes) {
                this.addTargetNode(injectorTarget, targetNodes, injectionPoint, insn);
            }
        }
        return targetNodes.values();
    }

    protected void addTargetNode(InjectorTarget target, Map<Integer, TargetNode> targetNodes, InjectionPoint injectionPoint, AbstractInsnNode insn) {
        Integer key = target.getTarget().indexOf(insn);
        TargetNode targetNode = targetNodes.get(key);
        if (targetNode == null) {
            targetNode = new TargetNode(insn);
            targetNodes.put(key, targetNode);
        }
        targetNode.nominators.add(injectionPoint);
    }

    protected boolean findTargetNodes(InjectorTarget target, InjectionPoint injectionPoint, Collection<AbstractInsnNode> nodes) {
        return injectionPoint.find(target.getDesc(), target.getSlice(injectionPoint), nodes);
    }

    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        if (target.classNode != this.classNode) {
            throw new InvalidInjectionException((ISelectorContext)this.info, "Target class does not match injector class in " + this);
        }
    }

    protected final void checkTargetModifiers(Target target, boolean exactMatch) {
        if (exactMatch && target.isStatic != this.isStatic) {
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("'static' modifier of handler method does not match target in %s", this));
        }
        if (!exactMatch && !this.isStatic && target.isStatic) {
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("non-static callback method %s targets a static method which is not supported", this));
        }
    }

    protected void checkTargetForNode(Target target, InjectionNodes.InjectionNode node, InjectionPoint.RestrictTargetLevel targetLevel) {
        if (target instanceof Constructor) {
            Constructor ctor = (Constructor)target;
            if (targetLevel == InjectionPoint.RestrictTargetLevel.METHODS_ONLY) {
                throw new InvalidInjectionException((ISelectorContext)this.info, String.format("Found %s targetting a constructor in injector %s", this.annotationType, this));
            }
            Bytecode.DelegateInitialiser superCall = ctor.findDelegateInitNode();
            if (!superCall.isPresent) {
                logger.warn("Suppressed mixin error for backwards compatibility: Delegate constructor lookup failed for %s target on %s", this.annotationType, this.info);
            } else {
                int superCallIndex = ctor.indexOf((AbstractInsnNode)superCall.insn);
                int targetIndex = ctor.indexOf(node.getCurrentTarget());
                if (targetIndex <= superCallIndex) {
                    if (targetLevel == InjectionPoint.RestrictTargetLevel.CONSTRUCTORS_AFTER_DELEGATE) {
                        throw new InvalidInjectionException((ISelectorContext)this.info, String.format("Found %s targetting a constructor before %s() in injector %s", this.annotationType, superCall, this));
                    }
                    if (!this.isStatic) {
                        throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s handler before %s() invocation must be static in injector %s", this.annotationType, superCall, this));
                    }
                    return;
                }
            }
        }
        this.checkTargetModifiers(target, false);
    }

    protected void preInject(Target target, InjectionNodes.InjectionNode node) {
    }

    protected abstract void inject(Target var1, InjectionNodes.InjectionNode var2);

    protected void postInject(Target target, InjectionNodes.InjectionNode node) {
    }

    protected AbstractInsnNode invokeHandler(InsnList insns) {
        return this.invokeHandler(insns, this.methodNode);
    }

    protected AbstractInsnNode invokeHandler(InsnList insns, MethodNode handler) {
        boolean isPrivate;
        boolean isInterface = Bytecode.hasFlag(this.classNode, 512);
        boolean bl = isPrivate = (handler.access & 2) != 0;
        int invokeOpcode = this.isStatic ? 184 : (isInterface ? 185 : (isPrivate ? 183 : 182));
        MethodInsnNode insn = new MethodInsnNode(invokeOpcode, this.classNode.name, handler.name, handler.desc, isInterface);
        insns.add((AbstractInsnNode)insn);
        this.info.addCallbackInvocation(handler);
        return insn;
    }

    protected AbstractInsnNode invokeHandlerWithArgs(Type[] args, InsnList insns, int[] argMap) {
        return this.invokeHandlerWithArgs(args, insns, argMap, 0, args.length);
    }

    protected AbstractInsnNode invokeHandlerWithArgs(Type[] args, InsnList insns, int[] argMap, int startArg, int endArg) {
        if (!this.isStatic) {
            insns.add((AbstractInsnNode)new VarInsnNode(25, 0));
        }
        this.pushArgs(args, insns, argMap, startArg, endArg);
        return this.invokeHandler(insns);
    }

    protected int[] storeArgs(Target target, Type[] args, InsnList insns, int start) {
        return this.storeArgs(target, args, insns, start, null, null);
    }

    protected int[] storeArgs(Target target, Type[] args, InsnList insns, int start, LabelNode from, LabelNode to) {
        int[] argMap = target.generateArgMap(args, start);
        this.storeArgs(target, args, insns, argMap, start, args.length, from, to);
        return argMap;
    }

    protected void storeArgs(Target target, Type[] args, InsnList insns, int[] argMap, int start, int end) {
        this.storeArgs(target, args, insns, argMap, start, end, null, null);
    }

    protected void storeArgs(Target target, Type[] args, InsnList insns, int[] argMap, int start, int end, LabelNode from, LabelNode to) {
        for (int arg = end - 1; arg >= start; --arg) {
            insns.add((AbstractInsnNode)new VarInsnNode(args[arg].getOpcode(54), argMap[arg]));
            target.addLocalVariable(argMap[arg], String.format("injectorAllocatedLocal%d", argMap[arg]), args[arg].getDescriptor(), from, to);
        }
    }

    protected void pushArgs(Type[] args, InsnList insns, int[] argMap, int start, int end) {
        this.pushArgs(args, insns, argMap, start, end, null);
    }

    protected void pushArgs(Type[] args, InsnList insns, int[] argMap, int start, int end, Target.Extension extension) {
        for (int arg = start; arg < end && arg < args.length; ++arg) {
            insns.add((AbstractInsnNode)new VarInsnNode(args[arg].getOpcode(21), argMap[arg]));
            if (extension == null) continue;
            extension.add(args[arg].getSize());
        }
    }

    protected final void validateParams(InjectorData injector, Type returnType, Type ... args) {
        String description = String.format("%s %s method %s from %s", this.annotationType, injector, this, this.info.getMixin());
        int argIndex = 0;
        try {
            injector.coerceReturnType = this.checkCoerce(-1, returnType, description, injector.allowCoerceArgs);
            for (Type arg : args) {
                if (arg == null) continue;
                this.checkCoerce(argIndex, arg, description, injector.allowCoerceArgs);
                ++argIndex;
            }
            if (argIndex == this.methodArgs.length) {
                return;
            }
            for (int targetArg = 0; targetArg < injector.target.arguments.length && argIndex < this.methodArgs.length; ++targetArg, ++argIndex) {
                this.checkCoerce(argIndex, injector.target.arguments[targetArg], description, true);
                ++injector.captureTargetArgs;
            }
        }
        catch (InvalidInjectionException ex) {
            String expected = this.methodArgs.length > args.length ? Bytecode.generateDescriptor(returnType, ObjectArrays.concat(args, injector.target.arguments, Type.class)) : Bytecode.generateDescriptor(returnType, args);
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s. Handler signature: %s Expected signature: %s", ex.getMessage(), this.methodNode.desc, expected));
        }
        if (argIndex < this.methodArgs.length) {
            Type[] extraArgs = Arrays.copyOfRange(this.methodArgs, argIndex, this.methodArgs.length);
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s has an invalid signature. Found %d unexpected additional method arguments: %s", description, this.methodArgs.length - argIndex, new SignaturePrinter(extraArgs).getFormattedArgs()));
        }
    }

    protected final boolean checkCoerce(int index, Type toType, String description, boolean allowCoercion) {
        String argIndex;
        if (index >= this.methodArgs.length) {
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s has an invalid signature. Not enough arguments: expected argument type %s at index %d", description, SignaturePrinter.getTypeName(toType), index));
        }
        Type fromType = index < 0 ? this.returnType : this.methodArgs[index];
        AnnotationNode coerce = Annotations.getInvisibleParameter(this.methodNode, Coerce.class, index);
        boolean isReturn = index < 0;
        String argType = isReturn ? "return" : "argument";
        String string = argIndex = isReturn ? "" : " at index " + index;
        if (fromType.equals((Object)toType)) {
            if (coerce != null && this.info.getMixin().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
                logger.info("Possibly-redundant @Coerce on {} {} type{}, {} is identical to {}", description, argType, argIndex, SignaturePrinter.getTypeName(toType), SignaturePrinter.getTypeName(fromType));
            }
            return false;
        }
        if (coerce == null || !allowCoercion) {
            String coerceWarning = coerce != null ? ". @Coerce not allowed here" : "";
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s has an invalid signature. Found unexpected %s type %s%s, expected %s%s", description, argType, SignaturePrinter.getTypeName(fromType), argIndex, SignaturePrinter.getTypeName(toType), coerceWarning));
        }
        boolean canCoerce = Injector.canCoerce(fromType, toType);
        if (!canCoerce) {
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s has an invalid signature. Cannot @Coerce %s type %s%s to %s", description, argType, SignaturePrinter.getTypeName(toType), argIndex, SignaturePrinter.getTypeName(fromType)));
        }
        return true;
    }

    protected void throwException(InsnList insns, Target.Extension extraStack, String exceptionType, String message) {
        insns.add((AbstractInsnNode)new TypeInsnNode(187, exceptionType));
        insns.add((AbstractInsnNode)new InsnNode(89));
        insns.add((AbstractInsnNode)new LdcInsnNode((Object)message));
        insns.add((AbstractInsnNode)new MethodInsnNode(183, exceptionType, "<init>", "(Ljava/lang/String;)V", false));
        insns.add((AbstractInsnNode)new InsnNode(191));
        extraStack.add(3);
    }

    public static boolean canCoerce(Type from, Type to) {
        int fromSort = from.getSort();
        int toSort = to.getSort();
        if (fromSort >= 9 && toSort >= 9 && fromSort == toSort) {
            if (fromSort == 9 && from.getDimensions() != to.getDimensions()) {
                return false;
            }
            return Injector.canCoerce(ClassInfo.forType(from, ClassInfo.TypeLookup.ELEMENT_TYPE), ClassInfo.forType(to, ClassInfo.TypeLookup.ELEMENT_TYPE));
        }
        return Injector.canCoerce(from.getDescriptor(), to.getDescriptor());
    }

    public static boolean canCoerce(String from, String to) {
        if (from.length() > 1 || to.length() > 1) {
            return false;
        }
        return Injector.canCoerce(from.charAt(0), to.charAt(0));
    }

    public static boolean canCoerce(char from, char to) {
        return to == 'I' && "IBSCZ".indexOf(from) > -1;
    }

    private static boolean canCoerce(ClassInfo from, ClassInfo to) {
        return from != null && to != null && (to == from || to.hasSuperClass(from, ClassInfo.Traversal.ALL, true));
    }

    public static class InjectorData {
        public final Target target;
        public String description;
        public boolean allowCoerceArgs;
        public int captureTargetArgs = 0;
        public boolean coerceReturnType = false;

        public InjectorData(Target target) {
            this(target, "handler");
        }

        public InjectorData(Target target, String description) {
            this(target, description, true);
        }

        public InjectorData(Target target, String description, boolean allowCoerceArgs) {
            this.target = target;
            this.description = description;
            this.allowCoerceArgs = allowCoerceArgs;
        }

        public String toString() {
            return this.description;
        }
    }

    public static final class TargetNode {
        final AbstractInsnNode insn;
        final Set<InjectionPoint> nominators = new HashSet<InjectionPoint>();

        TargetNode(AbstractInsnNode insn) {
            this.insn = insn;
        }

        public AbstractInsnNode getNode() {
            return this.insn;
        }

        public Set<InjectionPoint> getNominators() {
            return Collections.unmodifiableSet(this.nominators);
        }

        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != TargetNode.class) {
                return false;
            }
            return ((TargetNode)obj).insn == this.insn;
        }

        public int hashCode() {
            return this.insn.hashCode();
        }
    }
}

