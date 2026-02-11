package org.spongepowered.asm.mixin.transformer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.transformer.ActivityStack;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.TargetClassContext;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinApplicatorException;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ConstraintParser;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.throwables.ConstraintViolationException;
import org.spongepowered.asm.util.throwables.InvalidConstraintException;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;

class MixinApplicatorStandard {
    protected static final List<Class<? extends Annotation>> CONSTRAINED_ANNOTATIONS = ImmutableList.of(Overwrite.class, Inject.class, ModifyArg.class, ModifyArgs.class, Redirect.class, ModifyVariable.class, ModifyConstant.class);
    protected static final Set<Integer> ORDERS_NONE = ImmutableSet.of(Integer.valueOf(0));
    protected final ILogger logger = MixinService.getService().getLogger("mixin");
    protected final TargetClassContext context;
    protected final String targetName;
    protected final ClassNode targetClass;
    protected final ClassInfo targetClassInfo;
    protected final Profiler profiler = Profiler.getProfiler("mixin");
    protected final IMixinAuditTrail auditTrail;
    protected final ActivityStack activities = new ActivityStack();
    protected final boolean mergeSignatures;

    MixinApplicatorStandard(TargetClassContext context) {
        this.context = context;
        this.targetName = context.getClassName();
        this.targetClass = context.getClassNode();
        this.targetClassInfo = context.getClassInfo();
        ExtensionClassExporter exporter = (ExtensionClassExporter)context.getExtensions().getExtension(ExtensionClassExporter.class);
        this.mergeSignatures = exporter.isDecompilerActive() && MixinEnvironment.getCurrentEnvironment().getOption(MixinEnvironment.Option.DEBUG_EXPORT_DECOMPILE_MERGESIGNATURES);
        this.auditTrail = MixinService.getService().getAuditTrail();
    }

    final void apply(SortedSet<MixinInfo> mixins) {
        ArrayList<MixinTargetContext> mixinContexts = new ArrayList<MixinTargetContext>();
        Iterator iter = mixins.iterator();
        while (iter.hasNext()) {
            MixinInfo mixin = (MixinInfo)iter.next();
            try {
                this.logger.log(mixin.getLoggingLevel(), "Mixing {} from {} into {}", mixin.getName(), mixin.getParent(), this.targetName);
                mixinContexts.add(mixin.createContextFor(this.context));
                if (this.auditTrail == null) continue;
                this.auditTrail.onApply(this.targetName, mixin.toString());
            }
            catch (InvalidMixinException ex) {
                if (mixin.isRequired()) {
                    throw ex;
                }
                this.context.addSuppressed(ex);
                iter.remove();
            }
        }
        MixinTargetContext current = null;
        this.activities.clear();
        try {
            IActivityContext.IActivity activity = this.activities.begin("PreApply Phase");
            IActivityContext.IActivity preApplyActivity = this.activities.begin("Mixin");
            for (MixinTargetContext context : mixinContexts) {
                preApplyActivity.next(context.toString());
                current = context;
                current.preApply(this.targetName, this.targetClass);
            }
            preApplyActivity.end();
            for (ApplicatorPass pass : ApplicatorPass.values()) {
                activity.next("%s Applicator Phase", new Object[]{pass});
                Profiler.Section timer = this.profiler.begin("pass", pass.name().toLowerCase(Locale.ROOT));
                IActivityContext.IActivity applyActivity = this.activities.begin("Mixin");
                Set<Integer> orders = ORDERS_NONE;
                if (pass == ApplicatorPass.INJECT_APPLY) {
                    orders = new TreeSet<Integer>();
                    for (MixinTargetContext context : mixinContexts) {
                        context.getInjectorOrders(orders);
                    }
                }
                for (Integer injectorOrder : orders) {
                    Iterator iter2 = mixinContexts.iterator();
                    while (iter2.hasNext()) {
                        current = (MixinTargetContext)iter2.next();
                        applyActivity.next(current.toString());
                        try {
                            this.applyMixin(current, pass, injectorOrder);
                        }
                        catch (InvalidMixinException ex) {
                            if (current.isRequired()) {
                                throw ex;
                            }
                            this.context.addSuppressed(ex);
                            iter2.remove();
                        }
                    }
                }
                applyActivity.end();
                timer.end();
            }
            activity.next("PostApply Phase");
            IActivityContext.IActivity postApplyActivity = this.activities.begin("Mixin");
            Iterator iter3 = mixinContexts.iterator();
            while (iter3.hasNext()) {
                current = (MixinTargetContext)iter3.next();
                postApplyActivity.next(current.toString());
                try {
                    current.postApply(this.targetName, this.targetClass);
                }
                catch (InvalidMixinException ex) {
                    if (current.isRequired()) {
                        throw ex;
                    }
                    this.context.addSuppressed(ex);
                    iter3.remove();
                }
            }
            activity.end();
        }
        catch (InvalidMixinException ex) {
            ex.prepend(this.activities);
            throw ex;
        }
        catch (Exception ex) {
            throw new MixinApplicatorException(current, "Unexpecteded " + ex.getClass().getSimpleName() + " whilst applying the mixin class:", (Throwable)ex, (IActivityContext)this.activities);
        }
        this.applySourceMap(this.context);
        this.context.processDebugTasks();
    }

    protected final void applyMixin(MixinTargetContext mixin, ApplicatorPass pass, int injectorOrder) {
        IActivityContext.IActivity activity = this.activities.begin("Apply");
        switch (pass) {
            case MAIN: {
                activity.next("Apply Signature");
                this.applySignature(mixin);
                activity.next("Apply Interfaces");
                this.applyInterfaces(mixin);
                activity.next("Apply Attributess");
                this.applyAttributes(mixin);
                activity.next("Apply Annotations");
                this.applyAnnotations(mixin);
                activity.next("Apply Fields");
                this.applyFields(mixin);
                activity.next("Apply Methods");
                this.applyMethods(mixin);
                activity.next("Apply Initialisers");
                this.applyInitialisers(mixin);
                break;
            }
            case INJECT_PREPARE: {
                activity.next("Prepare Injections");
                this.prepareInjections(mixin);
                break;
            }
            case ACCESSOR: {
                activity.next("Apply Accessors");
                this.applyAccessors(mixin);
                break;
            }
            case INJECT_PREINJECT: {
                activity.next("Apply Injections");
                this.applyPreInjections(mixin);
                break;
            }
            case INJECT_APPLY: {
                activity.next("Apply Injections");
                this.applyInjections(mixin, injectorOrder);
                break;
            }
            default: {
                throw new IllegalStateException("Invalid pass specified " + (Object)((Object)pass));
            }
        }
        activity.end();
    }

    protected void applySignature(MixinTargetContext mixin) {
        if (this.mergeSignatures) {
            this.context.mergeSignature(mixin.getSignature());
        }
    }

    protected void applyInterfaces(MixinTargetContext mixin) {
        for (String interfaceName : mixin.getInterfaces()) {
            if (this.targetClass.interfaces.contains(interfaceName)) continue;
            this.targetClass.interfaces.add(interfaceName);
            this.targetClassInfo.addInterface(interfaceName);
        }
    }

    protected void applyAttributes(MixinTargetContext mixin) {
        int requiredVersion;
        if (mixin.shouldSetSourceFile()) {
            this.targetClass.sourceFile = mixin.getSourceFile();
        }
        if (((requiredVersion = mixin.getMinRequiredClassVersion()) & 0xFFFF) > (this.targetClass.version & 0xFFFF)) {
            this.targetClass.version = requiredVersion;
        }
    }

    protected void applyAnnotations(MixinTargetContext mixin) {
        ClassNode sourceClass = mixin.getClassNode();
        Annotations.merge(sourceClass, this.targetClass);
    }

    protected void applyFields(MixinTargetContext mixin) {
        this.mergeShadowFields(mixin);
        this.mergeNewFields(mixin);
    }

    protected void mergeShadowFields(MixinTargetContext mixin) {
        for (Map.Entry<FieldNode, ClassInfo.Field> entry : mixin.getShadowFields()) {
            FieldNode shadow = entry.getKey();
            FieldNode target = this.findTargetField(shadow);
            if (target == null) continue;
            Annotations.merge(shadow, target);
            if (!entry.getValue().isDecoratedMutable()) continue;
            target.access &= 0xFFFFFFEF;
        }
    }

    protected void mergeNewFields(MixinTargetContext mixin) {
        for (FieldNode field : mixin.getFields()) {
            FieldNode target = this.findTargetField(field);
            if (target != null) continue;
            this.targetClass.fields.add(field);
            mixin.fieldMerged(field);
            if (field.signature == null) continue;
            if (this.mergeSignatures) {
                SignatureVisitor sv = mixin.getSignature().getRemapper();
                new SignatureReader(field.signature).accept(sv);
                field.signature = sv.toString();
                continue;
            }
            field.signature = null;
        }
    }

    protected void applyMethods(MixinTargetContext mixin) {
        IActivityContext.IActivity activity = this.activities.begin("?");
        for (MethodNode shadow : mixin.getShadowMethods()) {
            activity.next("@Shadow %s:%s", shadow.desc, shadow.name);
            this.applyShadowMethod(mixin, shadow);
        }
        for (MethodNode mixinMethod : mixin.getMethods()) {
            activity.next("%s:%s", mixinMethod.desc, mixinMethod.name);
            this.applyNormalMethod(mixin, mixinMethod);
        }
        activity.end();
    }

    protected void applyShadowMethod(MixinTargetContext mixin, MethodNode shadow) {
        MethodNode target = this.findTargetMethod(shadow);
        if (target != null) {
            Annotations.merge(shadow, target);
        }
    }

    protected void applyNormalMethod(MixinTargetContext mixin, MethodNode mixinMethod) {
        mixin.transformMethod(mixinMethod);
        if (!mixinMethod.name.startsWith("<")) {
            this.checkMethodVisibility(mixin, mixinMethod);
            this.checkMethodConstraints(mixin, mixinMethod);
            this.mergeMethod(mixin, mixinMethod);
        } else if ("<clinit>".equals(mixinMethod.name)) {
            IActivityContext.IActivity activity = this.activities.begin("Merge CLINIT insns");
            this.appendInsns(mixin, mixinMethod);
            activity.end();
        }
    }

    protected void mergeMethod(MixinTargetContext mixin, MethodNode method) {
        boolean isOverwrite = Annotations.getVisible(method, Overwrite.class) != null;
        MethodNode target = this.findTargetMethod(method);
        if (target != null) {
            if (this.isAlreadyMerged(mixin, method, isOverwrite, target)) {
                return;
            }
            AnnotationNode intrinsic = Annotations.getInvisible(method, Intrinsic.class);
            if (intrinsic != null) {
                if (this.mergeIntrinsic(mixin, method, isOverwrite, target, intrinsic)) {
                    mixin.getTarget().methodMerged(method);
                    return;
                }
            } else {
                if (mixin.requireOverwriteAnnotations() && !isOverwrite) {
                    throw new InvalidMixinException((IMixinContext)mixin, String.format("%s%s in %s cannot overwrite method in %s because @Overwrite is required by the parent configuration", method.name, method.desc, mixin, mixin.getTarget().getClassName()));
                }
                this.targetClass.methods.remove(target);
            }
        } else if (isOverwrite) {
            throw new InvalidMixinException((IMixinContext)mixin, String.format("Overwrite target \"%s\" was not located in target class %s", method.name, mixin.getTargetClassRef()));
        }
        this.targetClass.methods.add(method);
        mixin.methodMerged(method);
        if (method.signature != null) {
            if (this.mergeSignatures) {
                SignatureVisitor sv = mixin.getSignature().getRemapper();
                new SignatureReader(method.signature).accept(sv);
                method.signature = sv.toString();
            } else {
                method.signature = null;
            }
        }
    }

    protected boolean isAlreadyMerged(MixinTargetContext mixin, MethodNode method, boolean isOverwrite, MethodNode target) {
        AnnotationNode accTarget;
        AnnotationNode merged = Annotations.getVisible(target, MixinMerged.class);
        if (merged == null) {
            if (Annotations.getVisible(target, Final.class) != null) {
                this.logger.warn("Overwrite prohibited for @Final method {} in {}. Skipping method.", method.name, mixin);
                return true;
            }
            return false;
        }
        String sessionId = (String)Annotations.getValue(merged, "sessionId");
        if (!this.context.getSessionId().equals(sessionId)) {
            throw new ClassFormatError("Invalid @MixinMerged annotation found in" + mixin + " at " + method.name + " in " + this.targetClass.name);
        }
        if (Bytecode.hasFlag(target, 4160) && Bytecode.hasFlag(method, 4160)) {
            if (mixin.getEnvironment().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
                this.logger.warn("Synthetic bridge method clash for {} in {}", method.name, mixin);
            }
            return true;
        }
        String owner = (String)Annotations.getValue(merged, "mixin");
        int priority = (Integer)Annotations.getValue(merged, "priority");
        AnnotationNode accMethod = Annotations.getSingleVisible(method, Accessor.class, Invoker.class);
        if (accMethod != null && (accTarget = Annotations.getSingleVisible(target, Accessor.class, Invoker.class)) != null) {
            String myTarget = (String)Annotations.getValue(accMethod, "target");
            String trTarget = (String)Annotations.getValue(accTarget, "target");
            if (myTarget == null) {
                throw new MixinError("Encountered undecorated Accessor method in " + mixin + " applying to " + this.targetName);
            }
            if (myTarget.equals(trTarget)) {
                return true;
            }
            throw new InvalidMixinException((IMixinContext)mixin, String.format("Incompatible @%s %s (for %s) in %s previously written by %s (for %s)", Annotations.getSimpleName(accMethod), method.name, myTarget, mixin, owner, trTarget));
        }
        if (priority >= mixin.getPriority() && !owner.equals(mixin.getClassName())) {
            this.logger.warn("Method overwrite conflict for {} in {}, previously written by {}. Skipping method.", method.name, mixin, owner);
            return true;
        }
        if (Annotations.getVisible(target, Final.class) != null) {
            this.logger.warn("Method overwrite conflict for @Final method {} in {} declared by {}. Skipping method.", method.name, mixin, owner);
            return true;
        }
        return false;
    }

    protected boolean mergeIntrinsic(MixinTargetContext mixin, MethodNode method, boolean isOverwrite, MethodNode target, AnnotationNode intrinsic) {
        AnnotationNode renamed;
        if (isOverwrite) {
            throw new InvalidMixinException((IMixinContext)mixin, "@Intrinsic is not compatible with @Overwrite, remove one of these annotations on " + method.name + " in " + mixin);
        }
        String methodName = method.name + method.desc;
        if (Bytecode.hasFlag(method, 8)) {
            throw new InvalidMixinException((IMixinContext)mixin, "@Intrinsic method cannot be static, found " + methodName + " in " + mixin);
        }
        if (!(Bytecode.hasFlag(method, 4096) || (renamed = Annotations.getVisible(method, MixinRenamed.class)) != null && Annotations.getValue(renamed, "isInterfaceMember", Boolean.FALSE).booleanValue())) {
            throw new InvalidMixinException((IMixinContext)mixin, "@Intrinsic method must be prefixed interface method, no rename encountered on " + methodName + " in " + mixin);
        }
        if (!Annotations.getValue(intrinsic, "displace", Boolean.FALSE).booleanValue()) {
            this.logger.log(mixin.getLoggingLevel(), "Skipping Intrinsic mixin method {} for {}", methodName, mixin.getTargetClassRef());
            return true;
        }
        this.displaceIntrinsic(mixin, method, target);
        return false;
    }

    protected void displaceIntrinsic(MixinTargetContext mixin, MethodNode method, MethodNode target) {
        String proxyName = "proxy+" + target.name;
        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof MethodInsnNode) || insn.getOpcode() == 184) continue;
            MethodInsnNode methodNode = (MethodInsnNode)insn;
            if (!methodNode.owner.equals(this.targetClass.name) || !methodNode.name.equals(target.name) || !methodNode.desc.equals(target.desc)) continue;
            methodNode.name = proxyName;
        }
        target.name = proxyName;
    }

    protected final void appendInsns(MixinTargetContext mixin, MethodNode method) {
        if (Type.getReturnType((String)method.desc) != Type.VOID_TYPE) {
            throw new IllegalArgumentException("Attempted to merge insns from a method which does not return void");
        }
        MethodNode target = this.findTargetMethod(method);
        if (target != null) {
            AbstractInsnNode returnNode = Bytecode.findInsn(target, 177);
            if (returnNode != null) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof LineNumberNode || insn.getOpcode() == 177) continue;
                    target.instructions.insertBefore(returnNode, insn);
                }
                target.maxLocals = Math.max(target.maxLocals, method.maxLocals);
                target.maxStack = Math.max(target.maxStack, method.maxStack);
            }
            return;
        }
        this.targetClass.methods.add(method);
    }

    protected void applyInitialisers(MixinTargetContext mixin) {
        Initialiser initialiser = mixin.getInitialiser();
        if (initialiser == null || initialiser.size() == 0) {
            return;
        }
        for (Constructor ctor : this.context.getConstructors()) {
            if (!ctor.isInjectable()) continue;
            int extraStack = initialiser.getMaxStack() - ctor.getMaxStack();
            if (extraStack > 0) {
                ctor.extendStack().add(extraStack);
            }
            initialiser.injectInto(ctor);
        }
    }

    protected void prepareInjections(MixinTargetContext mixin) {
        mixin.prepareInjections();
    }

    protected void applyPreInjections(MixinTargetContext mixin) {
        mixin.applyPreInjections();
    }

    protected void applyInjections(MixinTargetContext mixin, int injectorOrder) {
        mixin.applyInjections(injectorOrder);
    }

    protected void applyAccessors(MixinTargetContext mixin) {
        List<MethodNode> accessorMethods = mixin.generateAccessors();
        for (MethodNode method : accessorMethods) {
            if (method.name.startsWith("<")) continue;
            this.mergeMethod(mixin, method);
        }
    }

    protected void checkMethodVisibility(MixinTargetContext mixin, MethodNode mixinMethod) {
        if (Bytecode.hasFlag(mixinMethod, 8) && !Bytecode.hasFlag(mixinMethod, 2) && !Bytecode.hasFlag(mixinMethod, 4096) && Annotations.getVisible(mixinMethod, Overwrite.class) == null) {
            throw new InvalidMixinException((IMixinContext)mixin, String.format("Mixin %s contains non-private static method %s", mixin, mixinMethod));
        }
    }

    protected void applySourceMap(TargetClassContext context) {
        this.targetClass.sourceDebug = context.getSourceMap().toString();
    }

    protected void checkMethodConstraints(MixinTargetContext mixin, MethodNode method) {
        for (Class<? extends Annotation> annotationType : CONSTRAINED_ANNOTATIONS) {
            AnnotationNode annotation = Annotations.getVisible(method, annotationType);
            if (annotation == null) continue;
            this.checkConstraints(mixin, method, annotation);
        }
    }

    protected final void checkConstraints(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        try {
            ConstraintParser.Constraint constraint = ConstraintParser.parse(annotation);
            try {
                constraint.check(mixin.getEnvironment());
            }
            catch (ConstraintViolationException ex) {
                String message = String.format("Constraint violation: %s on %s in %s", ex.getMessage(), method, mixin);
                this.logger.warn(message, new Object[0]);
                if (!mixin.getEnvironment().getOption(MixinEnvironment.Option.IGNORE_CONSTRAINTS)) {
                    throw new InvalidMixinException((IMixinContext)mixin, message, (Throwable)ex);
                }
            }
        }
        catch (InvalidConstraintException ex) {
            throw new InvalidMixinException((IMixinContext)mixin, ex.getMessage());
        }
    }

    protected final MethodNode findTargetMethod(MethodNode searchFor) {
        for (MethodNode target : this.targetClass.methods) {
            if (!target.name.equals(searchFor.name) || !target.desc.equals(searchFor.desc)) continue;
            return target;
        }
        return null;
    }

    protected final FieldNode findTargetField(FieldNode searchFor) {
        for (FieldNode target : this.targetClass.fields) {
            if (!target.name.equals(searchFor.name) || !target.desc.equals(searchFor.desc)) continue;
            return target;
        }
        return null;
    }

    static enum ApplicatorPass {
        MAIN,
        INJECT_PREPARE,
        ACCESSOR,
        INJECT_PREINJECT,
        INJECT_APPLY;

    }
}

