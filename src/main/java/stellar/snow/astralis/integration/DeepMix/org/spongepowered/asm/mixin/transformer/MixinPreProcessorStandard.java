package org.spongepowered.asm.mixin.transformer;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.MemberRef;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ActivityStack;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.InnerClassGenerator;
import org.spongepowered.asm.mixin.transformer.InterfaceInfo;
import org.spongepowered.asm.mixin.transformer.MethodMapper;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.TargetClassContext;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinPreProcessorException;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.throwables.SyntheticBridgeException;
import org.spongepowered.include.com.google.common.base.Strings;

class MixinPreProcessorStandard {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    protected final MixinInfo mixin;
    protected final MixinInfo.MixinClassNode classNode;
    protected final MixinEnvironment env;
    protected final Profiler profiler = Profiler.getProfiler("mixin");
    protected final ActivityStack activities = new ActivityStack();
    private final boolean verboseLogging;
    private final boolean strictUnique;
    private boolean prepared;
    private boolean attached;

    MixinPreProcessorStandard(MixinInfo mixin, MixinInfo.MixinClassNode classNode) {
        this.mixin = mixin;
        this.classNode = classNode;
        this.env = mixin.getParent().getEnvironment();
        this.verboseLogging = this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
        this.strictUnique = this.env.getOption(MixinEnvironment.Option.DEBUG_UNIQUE);
    }

    final MixinPreProcessorStandard prepare(Extensions extensions) {
        if (this.prepared) {
            return this;
        }
        this.prepared = true;
        this.activities.clear();
        Profiler.Section prepareTimer = this.profiler.begin("prepare");
        try {
            IActivityContext.IActivity activity = this.activities.begin("Prepare inner classes");
            this.prepareInnerClasses(extensions);
            activity.next("Prepare method");
            for (MixinInfo.MixinMethodNode mixinMethod : this.classNode.mixinMethods) {
                ClassInfo.Method method = this.mixin.getClassInfo().findMethod(mixinMethod, mixinMethod.access | 0x40000);
                IActivityContext.IActivity methodActivity = this.activities.begin(mixinMethod.toString());
                this.prepareMethod(mixinMethod, method);
                methodActivity.end();
            }
            activity.next("Prepare field");
            for (FieldNode mixinField : this.classNode.fields) {
                IActivityContext.IActivity fieldActivity = this.activities.begin(String.format("%s:%s", mixinField.name, mixinField.desc));
                this.prepareField(mixinField);
                fieldActivity.end();
            }
            activity.end();
        }
        catch (MixinException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new MixinPreProcessorException(String.format("Prepare error for %s during activity:", this.mixin), ex, this.activities);
        }
        prepareTimer.end();
        return this;
    }

    protected void prepareInnerClasses(Extensions extensions) {
        InnerClassGenerator icg = (InnerClassGenerator)extensions.getGenerator(InnerClassGenerator.class);
        for (String targetClassName : this.mixin.getDeclaredTargetClasses()) {
            ClassInfo targetClassInfo = ClassInfo.forName(targetClassName);
            for (String innerClass : this.mixin.getInnerClasses()) {
                icg.registerInnerClass(this.mixin, targetClassInfo, innerClass);
            }
        }
    }

    protected void prepareMethod(MixinInfo.MixinMethodNode mixinMethod, ClassInfo.Method method) {
        this.prepareShadow(mixinMethod, method);
        this.prepareSoftImplements(mixinMethod, method);
    }

    protected void prepareShadow(MixinInfo.MixinMethodNode mixinMethod, ClassInfo.Method method) {
        AnnotationNode shadowAnnotation = Annotations.getVisible(mixinMethod, Shadow.class);
        if (shadowAnnotation == null) {
            return;
        }
        String prefix = (String)Annotations.getValue(shadowAnnotation, "prefix", Shadow.class);
        if (mixinMethod.name.startsWith(prefix)) {
            Annotations.setVisible(mixinMethod, MixinRenamed.class, "originalName", mixinMethod.name);
            String newName = mixinMethod.name.substring(prefix.length());
            mixinMethod.name = method.renameTo(newName);
        }
    }

    protected void prepareSoftImplements(MixinInfo.MixinMethodNode mixinMethod, ClassInfo.Method method) {
        for (InterfaceInfo iface : this.mixin.getSoftImplements()) {
            if (!iface.renameMethod(mixinMethod)) continue;
            method.renameTo(mixinMethod.name);
        }
    }

    protected void prepareField(FieldNode mixinField) {
    }

    final MixinPreProcessorStandard conform(TargetClassContext target) {
        return this.conform(target.getClassInfo());
    }

    final MixinPreProcessorStandard conform(ClassInfo target) {
        this.activities.clear();
        Profiler.Section conformTimer = this.profiler.begin("conform");
        try {
            for (MixinInfo.MixinMethodNode mixinMethod : this.classNode.mixinMethods) {
                if (!mixinMethod.isInjector()) continue;
                ClassInfo.Method method = this.mixin.getClassInfo().findMethod(mixinMethod, 10);
                IActivityContext.IActivity methodActivity = this.activities.begin("Conform injector %s", new Object[]{mixinMethod});
                this.conformInjector(target, mixinMethod, method);
                methodActivity.end();
            }
        }
        catch (MixinException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new MixinPreProcessorException(String.format("Conform error for %s during activity:", this.mixin), ex, this.activities);
        }
        conformTimer.end();
        return this;
    }

    private void conformInjector(ClassInfo targetClass, MixinInfo.MixinMethodNode mixinMethod, ClassInfo.Method method) {
        MethodMapper methodMapper = targetClass.getMethodMapper();
        methodMapper.remapHandlerMethod(this.mixin, mixinMethod, method);
    }

    MixinTargetContext createContextFor(TargetClassContext target) {
        MixinTargetContext context = new MixinTargetContext(this.mixin, this.classNode, target);
        this.conform(target);
        this.attach(context);
        return context;
    }

    final MixinPreProcessorStandard attach(MixinTargetContext context) {
        if (this.attached) {
            throw new IllegalStateException("Preprocessor was already attached");
        }
        this.attached = true;
        this.activities.clear();
        Profiler.Section attachTimer = this.profiler.begin("attach");
        try {
            Profiler.Section timer = this.profiler.begin("methods");
            IActivityContext.IActivity activity = this.activities.begin("Attach method");
            this.attachMethods(context);
            timer = timer.next("fields");
            activity.next("Attach field");
            this.attachFields(context);
            timer = timer.next("transform");
            activity.next("Transform");
            this.transform(context);
            activity.end();
            timer.end();
        }
        catch (MixinException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new MixinPreProcessorException(String.format("Attach error for %s during activity:", this.mixin), ex, this.activities);
        }
        attachTimer.end();
        return this;
    }

    protected void attachMethods(MixinTargetContext context) {
        IActivityContext.IActivity methodActivity = this.activities.begin("?");
        Iterator<MixinInfo.MixinMethodNode> iter = this.classNode.mixinMethods.iterator();
        while (iter.hasNext()) {
            MixinInfo.MixinMethodNode mixinMethod = iter.next();
            methodActivity.next(mixinMethod.toString());
            if (!this.validateMethod(context, mixinMethod)) {
                iter.remove();
                continue;
            }
            if (this.attachInjectorMethod(context, mixinMethod)) {
                context.addMixinMethod(mixinMethod);
                continue;
            }
            if (this.attachAccessorMethod(context, mixinMethod)) {
                iter.remove();
                continue;
            }
            if (this.attachShadowMethod(context, mixinMethod)) {
                context.addShadowMethod(mixinMethod);
                iter.remove();
                continue;
            }
            if (this.attachOverwriteMethod(context, mixinMethod)) {
                context.addMixinMethod(mixinMethod);
                continue;
            }
            if (this.attachUniqueMethod(context, mixinMethod)) {
                iter.remove();
                continue;
            }
            this.attachMethod(context, mixinMethod);
            context.addMixinMethod(mixinMethod);
        }
        methodActivity.end();
    }

    protected boolean validateMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
        return true;
    }

    protected boolean attachInjectorMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
        return mixinMethod.isInjector();
    }

    protected boolean attachAccessorMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
        return this.attachAccessorMethod(context, mixinMethod, SpecialMethod.ACCESSOR) || this.attachAccessorMethod(context, mixinMethod, SpecialMethod.INVOKER);
    }

    protected boolean attachAccessorMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod, SpecialMethod type) {
        AnnotationNode annotation = mixinMethod.getVisibleAnnotation(type.annotation);
        if (annotation == null) {
            return false;
        }
        String description = (Object)((Object)type) + " method " + mixinMethod.name;
        ClassInfo.Method method = this.getSpecialMethod(mixinMethod, type);
        if (MixinEnvironment.getCompatibilityLevel().isAtLeast(MixinEnvironment.CompatibilityLevel.JAVA_8) && method.isStatic()) {
            if (this.mixin.getTargets().size() > 1) {
                throw new InvalidAccessorException((IMixinContext)context, description + " in multi-target mixin is invalid. Mixin must have exactly 1 target.");
            }
            if (method.isConformed()) {
                mixinMethod.name = method.getName();
            } else {
                String uniqueName = context.getUniqueName(mixinMethod, true);
                logger.log(this.mixin.getLoggingLevel(), "Renaming @{} method {}{} to {} in {}", Annotations.getSimpleName(annotation), mixinMethod.name, mixinMethod.desc, uniqueName, this.mixin);
                mixinMethod.name = method.conform(uniqueName);
            }
        } else {
            if (!method.isAbstract()) {
                throw new InvalidAccessorException((IMixinContext)context, description + " is not abstract");
            }
            if (method.isStatic()) {
                throw new InvalidAccessorException((IMixinContext)context, description + " cannot be static");
            }
        }
        context.addAccessorMethod(mixinMethod, type.annotation);
        return true;
    }

    protected boolean attachShadowMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
        return this.attachSpecialMethod(context, mixinMethod, SpecialMethod.SHADOW);
    }

    protected boolean attachOverwriteMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
        return this.attachSpecialMethod(context, mixinMethod, SpecialMethod.OVERWRITE);
    }

    protected boolean attachSpecialMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod, SpecialMethod type) {
        AnnotationNode annotation = mixinMethod.getVisibleAnnotation(type.annotation);
        if (annotation == null) {
            return false;
        }
        if (type.isOverwrite) {
            this.checkMixinNotUnique(mixinMethod, type);
        }
        ClassInfo.Method method = this.getSpecialMethod(mixinMethod, type);
        MethodNode target = context.findMethod(mixinMethod, annotation);
        if (target == null) {
            target = context.findRemappedMethod(mixinMethod);
            if (target == null) {
                throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("%s method %s in %s was not located in the target class %s. %s%s", new Object[]{type, mixinMethod.name, this.mixin, context.getTarget(), context.getReferenceMapper().getStatus(), MixinPreProcessorStandard.getDynamicInfo(mixinMethod)}));
            }
            mixinMethod.name = method.renameTo(target.name);
        }
        if ("<init>".equals(target.name)) {
            throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("Nice try! %s in %s cannot alias a constructor", mixinMethod.name, this.mixin));
        }
        if (!Bytecode.compareFlags(mixinMethod, target, 8)) {
            throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("STATIC modifier of %s method %s in %s does not match the target", new Object[]{type, mixinMethod.name, this.mixin}));
        }
        this.conformVisibility(context, mixinMethod, type, target);
        if (!target.name.equals(mixinMethod.name)) {
            if (type.isOverwrite && (target.access & 2) == 0) {
                throw new InvalidMixinException((IMixinInfo)this.mixin, "Non-private method cannot be aliased. Found " + target.name);
            }
            mixinMethod.name = method.renameTo(target.name);
        }
        return true;
    }

    private void conformVisibility(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod, SpecialMethod type, MethodNode target) {
        Bytecode.Visibility visTarget = Bytecode.getVisibility(target);
        Bytecode.Visibility visMethod = Bytecode.getVisibility(mixinMethod);
        if (visMethod.ordinal() >= visTarget.ordinal()) {
            if (visTarget == Bytecode.Visibility.PRIVATE && visMethod.ordinal() > Bytecode.Visibility.PRIVATE.ordinal()) {
                context.getTarget().addUpgradedMethod(target);
            }
            return;
        }
        String message = String.format("%s %s method %s in %s cannot reduce visibiliy of %s target method", new Object[]{visMethod, type, mixinMethod.name, this.mixin, visTarget});
        if (type.isOverwrite && !this.mixin.getParent().conformOverwriteVisibility()) {
            throw new InvalidMixinException((IMixinInfo)this.mixin, message);
        }
        if (visMethod == Bytecode.Visibility.PRIVATE) {
            if (type.isOverwrite) {
                logger.warn("Static binding violation: {}, visibility will be upgraded.", message);
            }
            context.addUpgradedMethod(mixinMethod);
            Bytecode.setVisibility((MethodNode)mixinMethod, visTarget);
        }
    }

    protected ClassInfo.Method getSpecialMethod(MixinInfo.MixinMethodNode mixinMethod, SpecialMethod type) {
        ClassInfo.Method method = this.mixin.getClassInfo().findMethod(mixinMethod, 10);
        this.checkMethodNotUnique(method, type);
        return method;
    }

    protected void checkMethodNotUnique(ClassInfo.Method method, SpecialMethod type) {
        if (method.isUnique()) {
            throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("%s method %s in %s cannot be @Unique", new Object[]{type, method.getName(), this.mixin}));
        }
    }

    protected void checkMixinNotUnique(MixinInfo.MixinMethodNode mixinMethod, SpecialMethod type) {
        if (this.mixin.isUnique()) {
            throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("%s method %s found in a @Unique mixin %s", new Object[]{type, mixinMethod.name, this.mixin}));
        }
    }

    protected boolean attachUniqueMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
        String type;
        MethodNode target;
        ClassInfo.Method method = this.mixin.getClassInfo().findMethod(mixinMethod, 10);
        if (method == null || !method.isUnique() && !this.mixin.isUnique() && !method.isSynthetic()) {
            return false;
        }
        boolean synthetic = method.isSynthetic();
        if (synthetic) {
            context.transformDescriptor(mixinMethod);
            method.remapTo(mixinMethod.desc);
        }
        if ((target = context.findMethod(mixinMethod, null)) == null && !synthetic) {
            return false;
        }
        String string = type = synthetic ? "synthetic" : "@Unique";
        if (Bytecode.getVisibility(mixinMethod).ordinal() < Bytecode.Visibility.PUBLIC.ordinal()) {
            if (method.isConformed()) {
                mixinMethod.name = method.getName();
            } else {
                String uniqueName = context.getUniqueName(mixinMethod, false);
                logger.log(this.mixin.getLoggingLevel(), "Renaming {} method {}{} to {} in {}", type, mixinMethod.name, mixinMethod.desc, uniqueName, this.mixin);
                mixinMethod.name = method.conform(uniqueName);
            }
            return false;
        }
        if (target == null) {
            return false;
        }
        if (this.strictUnique) {
            throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("Method conflict, %s method %s in %s cannot overwrite %s%s in %s", type, mixinMethod.name, this.mixin, target.name, target.desc, context.getTarget()));
        }
        AnnotationNode unique = Annotations.getVisible(mixinMethod, Unique.class);
        if (unique == null || !Annotations.getValue(unique, "silent", Boolean.FALSE).booleanValue()) {
            if (Bytecode.hasFlag(mixinMethod, 64)) {
                try {
                    Bytecode.compareBridgeMethods(target, mixinMethod);
                    logger.debug("Discarding sythetic bridge method {} in {} because existing method in {} is compatible", type, mixinMethod.name, this.mixin, context.getTarget());
                    return true;
                }
                catch (SyntheticBridgeException ex) {
                    if (this.verboseLogging || this.env.getOption(MixinEnvironment.Option.DEBUG_VERIFY)) {
                        ex.printAnalysis(context, target, mixinMethod);
                    }
                    throw new InvalidMixinException((IMixinInfo)this.mixin, ex.getMessage());
                }
            }
            logger.warn("Discarding {} public method {} in {} because it already exists in {}", type, mixinMethod.name, this.mixin, context.getTarget());
            return true;
        }
        context.addMixinMethod(mixinMethod);
        return true;
    }

    protected void attachMethod(MixinTargetContext context, MixinInfo.MixinMethodNode mixinMethod) {
        MethodNode target;
        ClassInfo.Method method = this.mixin.getClassInfo().findMethod(mixinMethod);
        if (method == null) {
            return;
        }
        ClassInfo.Method parentMethod = this.mixin.getClassInfo().findMethodInHierarchy(mixinMethod, ClassInfo.SearchType.SUPER_CLASSES_ONLY);
        if (parentMethod != null && parentMethod.isRenamed()) {
            mixinMethod.name = method.renameTo(parentMethod.getName());
        }
        if ((target = context.findMethod(mixinMethod, null)) != null) {
            this.conformVisibility(context, mixinMethod, SpecialMethod.MERGE, target);
        }
    }

    protected void attachFields(MixinTargetContext context) {
        IActivityContext.IActivity fieldActivity = this.activities.begin("?");
        Iterator<FieldNode> iter = this.classNode.getFields().iterator();
        while (iter.hasNext()) {
            boolean isShadow;
            FieldNode mixinField = iter.next();
            fieldActivity.next("%s:%s", mixinField.name, mixinField.desc);
            AnnotationNode shadow = Annotations.getVisible(mixinField, Shadow.class);
            boolean bl = isShadow = shadow != null;
            if (!this.validateField(context, mixinField, shadow)) {
                iter.remove();
                continue;
            }
            ClassInfo.Field field = this.mixin.getClassInfo().findField(mixinField);
            context.transformDescriptor(mixinField);
            field.remapTo(mixinField.desc);
            if (field.isUnique() && isShadow) {
                throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("@Shadow field %s cannot be @Unique", mixinField.name));
            }
            FieldNode target = context.findField(mixinField, shadow);
            if (target == null) {
                if (shadow == null) {
                    context.addMixinField(mixinField);
                    continue;
                }
                target = context.findRemappedField(mixinField);
                if (target == null) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("@Shadow field %s was not located in the target class %s. %s%s", mixinField.name, context.getTarget(), context.getReferenceMapper().getStatus(), MixinPreProcessorStandard.getDynamicInfo(mixinField)));
                }
                mixinField.name = field.renameTo(target.name);
            }
            if (field.isUnique()) {
                if (Bytecode.getVisibility(mixinField).isLessThan(Bytecode.Visibility.PUBLIC)) {
                    String uniqueName = context.getUniqueName(mixinField);
                    logger.log(this.mixin.getLoggingLevel(), "Renaming @Unique field {}{} to {} in {}", mixinField.name, mixinField.desc, uniqueName, this.mixin);
                    mixinField.name = field.renameTo(uniqueName);
                    context.addMixinField(mixinField);
                    continue;
                }
                if (this.strictUnique) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("Field conflict, @Unique field %s in %s cannot overwrite %s%s in %s", mixinField.name, this.mixin, target.name, target.desc, context.getTarget()));
                }
                logger.warn("Discarding @Unique public field {} in {} because it already exists in {}. Note that declared FIELD INITIALISERS will NOT be removed!", mixinField.name, this.mixin, context.getTarget());
                iter.remove();
                continue;
            }
            if (!Bytecode.compareFlags(mixinField, target, 8)) {
                if (isShadow) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("STATIC modifier of @Shadow field %s in %s does not match the target", mixinField.name, this.mixin));
                }
                throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("Field %s in %s conflicts with %sstatic field in the target (%s)", mixinField.name, this.mixin, Bytecode.isStatic(target) ? "" : "non-", context.getTarget()));
            }
            if (!Bytecode.compareFlags(mixinField, target, 8)) {
                throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("STATIC modifier of @Shadow field %s in %s does not match the target", mixinField.name, this.mixin));
            }
            if (!target.desc.equals(mixinField.desc)) {
                throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("The field %s in the target class has a conflicting signature", mixinField.name));
            }
            if (!target.name.equals(mixinField.name)) {
                if ((target.access & 2) == 0 && (target.access & 0x1000) == 0) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, "Non-private field cannot be aliased. Found " + target.name);
                }
                mixinField.name = field.renameTo(target.name);
            }
            iter.remove();
            if (!isShadow) continue;
            boolean isFinal = field.isDecoratedFinal();
            if (this.verboseLogging && Bytecode.hasFlag(target, 16) != isFinal) {
                String message = isFinal ? "@Shadow field {}::{} is decorated with @Final but target is not final" : "@Shadow target {}::{} is final but shadow is not decorated with @Final";
                logger.warn(message, this.mixin, mixinField.name);
            }
            context.addShadowField(mixinField, field);
        }
    }

    protected boolean validateField(MixinTargetContext context, FieldNode field, AnnotationNode shadow) {
        if (Bytecode.isStatic(field) && !Bytecode.hasFlag(field, 2) && !Bytecode.hasFlag(field, 4096) && shadow == null) {
            throw new InvalidMixinException((IMixinContext)context, String.format("Mixin %s contains non-private static field %s:%s", context, field.name, field.desc));
        }
        String prefix = (String)Annotations.getValue(shadow, "prefix", Shadow.class);
        if (field.name.startsWith(prefix)) {
            throw new InvalidMixinException((IMixinContext)context, String.format("@Shadow field %s.%s has a shadow prefix. This is not allowed.", context, field.name));
        }
        if ("super$".equals(field.name)) {
            if (field.access != 2) {
                throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("Imaginary super field %s.%s must be private and non-final", context, field.name));
            }
            if (!field.desc.equals("L" + this.mixin.getClassRef() + ";")) {
                throw new InvalidMixinException((IMixinInfo)this.mixin, String.format("Imaginary super field %s.%s must have the same type as the parent mixin (%s)", context, field.name, this.mixin.getClassName()));
            }
            return false;
        }
        return true;
    }

    protected void transform(MixinTargetContext context) {
        IActivityContext.IActivity methodActivity = this.activities.begin("method");
        for (MethodNode mixinMethod : this.classNode.methods) {
            methodActivity.next("Method %s", mixinMethod);
            for (AbstractInsnNode insn : mixinMethod.instructions) {
                IActivityContext.IActivity activity = this.activities.begin(Bytecode.getOpcodeName(insn));
                if (insn instanceof MethodInsnNode) {
                    this.transformMethod((MethodInsnNode)insn);
                } else if (insn instanceof FieldInsnNode) {
                    this.transformField((FieldInsnNode)insn);
                } else if (insn instanceof InvokeDynamicInsnNode) {
                    this.transformInvokeDynamic((InvokeDynamicInsnNode)insn);
                }
                activity.end();
            }
        }
        methodActivity.end();
    }

    protected void transformInvokeDynamic(InvokeDynamicInsnNode invokeDynamicNode) {
        IActivityContext.IActivity activity = this.activities.begin("%s%s", invokeDynamicNode.name, invokeDynamicNode.desc);
        Profiler.Section metaTimer = this.profiler.begin("meta");
        MemberRef.Handle ref = new MemberRef.Handle(invokeDynamicNode.bsm);
        this.transformMemberReference(ref);
        invokeDynamicNode.bsm = ref.getMethodHandle();
        for (int i = 0; i < invokeDynamicNode.bsmArgs.length; ++i) {
            if (!(invokeDynamicNode.bsmArgs[i] instanceof Handle)) continue;
            ref = new MemberRef.Handle((Handle)invokeDynamicNode.bsmArgs[i]);
            this.transformMemberReference(ref);
            invokeDynamicNode.bsmArgs[i] = ref.getMethodHandle();
        }
        metaTimer.end();
        activity.end();
    }

    protected void transformMethod(MethodInsnNode methodNode) {
        MemberRef.Method ref = new MemberRef.Method(methodNode);
        this.transformMemberReference(ref);
    }

    protected void transformField(FieldInsnNode fieldNode) {
        MemberRef.Field ref = new MemberRef.Field(fieldNode);
        this.transformMemberReference(ref);
    }

    protected void transformMemberReference(MemberRef ref) {
        ClassInfo.Field member;
        IActivityContext.IActivity activity = this.activities.begin("%s::%s:%s", ref.getOwner(), ref.getName(), ref.getDesc());
        Profiler.Section metaTimer = this.profiler.begin("meta");
        ClassInfo owner = ClassInfo.forDescriptor(ref.getOwner(), ClassInfo.TypeLookup.DECLARED_TYPE);
        if (owner == null) {
            throw new RuntimeException(new ClassNotFoundException(ref.getOwner().replace('/', '.')));
        }
        if (ref.isField()) {
            int includeStatic = ref.getOpcode() == 178 || ref.getOpcode() == 179 ? 8 : 0;
            member = owner.findFieldInHierarchy(ref.getName(), ref.getDesc(), ClassInfo.SearchType.ALL_CLASSES, 2 | includeStatic);
        } else {
            int includeStatic = ref.getOpcode() == 184 ? 8 : 0;
            ClassInfo.Method method = owner.findMethodInHierarchy(ref.getName(), ref.getDesc(), ClassInfo.SearchType.ALL_CLASSES, 2 | includeStatic);
            member = method != null && !method.isAccessor() ? method : null;
        }
        metaTimer.end();
        if (member != null && ((ClassInfo.Member)member).isRenamed()) {
            ref.setName(((ClassInfo.Member)member).getName());
        }
        activity.end();
    }

    protected static String getDynamicInfo(MethodNode method) {
        return MixinPreProcessorStandard.getDynamicInfo("Method", Annotations.getInvisible(method, Dynamic.class));
    }

    protected static String getDynamicInfo(FieldNode method) {
        return MixinPreProcessorStandard.getDynamicInfo("Field", Annotations.getInvisible(method, Dynamic.class));
    }

    private static String getDynamicInfo(String targetType, AnnotationNode annotation) {
        String description = Strings.nullToEmpty((String)Annotations.getValue(annotation));
        Type upstream = (Type)Annotations.getValue(annotation, "mixin");
        if (upstream != null) {
            description = String.format("{%s} %s", upstream.getClassName(), description).trim();
        }
        return description.length() > 0 ? String.format(" %s is @Dynamic(%s)", targetType, description) : "";
    }

    static enum SpecialMethod {
        MERGE(true),
        OVERWRITE(true, Overwrite.class),
        SHADOW(false, Shadow.class),
        ACCESSOR(false, Accessor.class),
        INVOKER(false, Invoker.class);

        final boolean isOverwrite;
        final Class<? extends Annotation> annotation;
        final String description;

        private SpecialMethod(boolean isOverwrite, Class<? extends Annotation> type) {
            this.isOverwrite = isOverwrite;
            this.annotation = type;
            this.description = "@" + Annotations.getSimpleName(type);
        }

        private SpecialMethod(boolean isOverwrite) {
            this.isOverwrite = isOverwrite;
            this.annotation = null;
            this.description = "overwrite";
        }

        public String toString() {
            return this.description;
        }
    }
}

