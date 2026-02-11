package org.spongepowered.asm.mixin.transformer;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.struct.SourceMap;
import org.spongepowered.asm.mixin.transformer.ClassContext;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinApplicatorInterface;
import org.spongepowered.asm.mixin.transformer.MixinApplicatorStandard;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.mixin.transformer.MixinProcessor;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckClass;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;
import org.spongepowered.asm.util.perf.Profiler;

final class TargetClassContext
extends ClassContext
implements ITargetClassContext {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private final MixinEnvironment env;
    private final Extensions extensions;
    private final Profiler profiler;
    private final String sessionId;
    private final String className;
    private final ClassNode classNode;
    private final ClassInfo classInfo;
    private final SourceMap sourceMap;
    private final ClassSignature signature;
    private final SortedSet<MixinInfo> mixins;
    private final Map<String, Target> targetMethods = new HashMap<String, Target>();
    private final Set<MethodNode> mixinMethods = new HashSet<MethodNode>();
    private final Set<FieldNode> mixinFields = new HashSet<FieldNode>();
    private final List<InvalidMixinException> suppressedExceptions = new ArrayList<InvalidMixinException>();
    private boolean applied;
    private boolean export;
    private boolean forceExport;

    TargetClassContext(MixinEnvironment env, Extensions extensions, String sessionId, String name, ClassNode classNode, SortedSet<MixinInfo> mixins) {
        this.env = env;
        this.extensions = extensions;
        this.profiler = Profiler.getProfiler("mixin");
        this.sessionId = sessionId;
        this.className = name;
        this.classNode = classNode;
        this.classInfo = ClassInfo.fromClassNode(classNode);
        this.signature = this.classInfo.getSignature();
        this.mixins = mixins;
        this.sourceMap = new SourceMap(classNode.sourceFile);
        this.sourceMap.addFile(this.classNode);
    }

    public String toString() {
        return this.className;
    }

    boolean isExported() {
        return this.export;
    }

    boolean isExportForced() {
        return this.forceExport;
    }

    Extensions getExtensions() {
        return this.extensions;
    }

    String getSessionId() {
        return this.sessionId;
    }

    @Override
    String getClassRef() {
        return this.classNode.name;
    }

    String getClassName() {
        return this.className;
    }

    @Override
    public ClassNode getClassNode() {
        return this.classNode;
    }

    List<Constructor> getConstructors() {
        ArrayList<Constructor> ctors = new ArrayList<Constructor>();
        for (MethodNode method : this.classNode.methods) {
            if (!"<init>".equals(method.name)) continue;
            ctors.add((Constructor)this.getTargetMethod(method));
        }
        return ctors;
    }

    @Override
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }

    SourceMap getSourceMap() {
        return this.sourceMap;
    }

    void mergeSignature(ClassSignature signature) {
        this.signature.merge(signature);
    }

    void addMixinMethod(MethodNode method) {
        this.mixinMethods.add(method);
    }

    void methodMerged(MethodNode method) {
        if (!this.mixinMethods.remove(method)) {
            logger.debug("Unexpected: Merged unregistered method {}{} in {}", method.name, method.desc, this);
        }
    }

    void addMixinField(FieldNode field) {
        this.mixinFields.add(field);
    }

    void fieldMerged(FieldNode field) {
        if (!this.mixinFields.remove(field)) {
            logger.debug("Unexpected: Merged unregistered field {} {} in {}", field.desc, field.name, this);
        }
    }

    MethodNode findMethod(Deque<String> aliases, String desc) {
        return this.findAliasedMethod(aliases, desc, true);
    }

    MethodNode findAliasedMethod(Deque<String> aliases, String desc) {
        return this.findAliasedMethod(aliases, desc, false);
    }

    private MethodNode findAliasedMethod(Deque<String> aliases, String desc, boolean includeMixinMethods) {
        String alias = aliases.poll();
        if (alias == null) {
            return null;
        }
        for (MethodNode target : this.classNode.methods) {
            if (!target.name.equals(alias) || !target.desc.equals(desc)) continue;
            return target;
        }
        if (includeMixinMethods) {
            for (MethodNode target : this.mixinMethods) {
                if (!target.name.equals(alias) || !target.desc.equals(desc)) continue;
                return target;
            }
        }
        return this.findAliasedMethod(aliases, desc);
    }

    FieldNode findField(Deque<String> aliases, String desc) {
        return this.findAliasedField(aliases, desc, true);
    }

    FieldNode findAliasedField(Deque<String> aliases, String desc) {
        return this.findAliasedField(aliases, desc, false);
    }

    private FieldNode findAliasedField(Deque<String> aliases, String desc, boolean includeMixinFields) {
        String alias = aliases.poll();
        if (alias == null) {
            return null;
        }
        for (FieldNode target : this.classNode.fields) {
            if (!target.name.equals(alias) || !target.desc.equals(desc)) continue;
            return target;
        }
        if (includeMixinFields) {
            for (FieldNode target : this.mixinFields) {
                if (!target.name.equals(alias) || !target.desc.equals(desc)) continue;
                return target;
            }
        }
        return this.findAliasedField(aliases, desc, includeMixinFields);
    }

    Target getTargetMethod(MethodNode method) {
        if (!this.classNode.methods.contains(method)) {
            throw new IllegalArgumentException("Invalid target method supplied to getTargetMethod()");
        }
        String targetName = method.name + method.desc;
        Target target = this.targetMethods.get(targetName);
        if (target == null) {
            target = Target.of(this.classInfo, this.classNode, method);
            this.targetMethods.put(targetName, target);
        }
        return target;
    }

    void applyMixins() {
        if (this.applied) {
            throw new IllegalStateException("Mixins already applied to target class " + this.className);
        }
        this.applied = true;
        Profiler.Section timer = this.profiler.begin("preapply");
        this.preApply();
        timer = timer.next("apply");
        this.apply();
        timer = timer.next("postapply");
        this.postApply();
        timer.end();
    }

    private void preApply() {
        this.getClassInfo().getMethodMapper().reset();
        this.extensions.preApply(this);
    }

    private void apply() {
        MixinApplicatorStandard applicator = this.createApplicator();
        applicator.apply(this.mixins);
        this.applySignature();
        this.upgradeMethods();
        this.checkMerges();
    }

    private void postApply() {
        try {
            this.extensions.postApply(this);
            this.export = true;
        }
        catch (ExtensionCheckClass.ValidationFailedException ex) {
            MixinProcessor.logger.info(ex.getMessage(), new Object[0]);
            this.export |= this.forceExport || this.env.getOption(MixinEnvironment.Option.DEBUG_EXPORT);
        }
    }

    private MixinApplicatorStandard createApplicator() {
        if (this.classInfo.isInterface()) {
            return new MixinApplicatorInterface(this);
        }
        return new MixinApplicatorStandard(this);
    }

    private void applySignature() {
        this.classNode.signature = this.signature.toString();
    }

    private void checkMerges() {
        for (MethodNode method : this.mixinMethods) {
            if (method.name.startsWith("<")) continue;
            logger.debug("Unexpected: Registered method {}{} in {} was not merged", method.name, method.desc, this);
        }
        for (FieldNode field : this.mixinFields) {
            logger.debug("Unexpected: Registered field {} {} in {} was not merged", field.desc, field.name, this);
        }
    }

    void processDebugTasks() {
        AnnotationNode classDebugAnnotation = Annotations.getVisible(this.classNode, Debug.class);
        boolean bl = this.forceExport = classDebugAnnotation != null && Boolean.TRUE.equals(Annotations.getValue(classDebugAnnotation, "export"));
        if (!this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
            return;
        }
        if (classDebugAnnotation != null && Boolean.TRUE.equals(Annotations.getValue(classDebugAnnotation, "print"))) {
            Bytecode.textify(this.classNode, (OutputStream)System.err);
        }
        for (MethodNode method : this.classNode.methods) {
            AnnotationNode methodDebugAnnotation = Annotations.getVisible(method, Debug.class);
            if (methodDebugAnnotation == null || !Boolean.TRUE.equals(Annotations.getValue(methodDebugAnnotation, "print"))) continue;
            Bytecode.textify(method, (OutputStream)System.err);
        }
    }

    void addSuppressed(InvalidMixinException ex) {
        this.suppressedExceptions.add(ex);
    }

    List<InvalidMixinException> getSuppressedExceptions() {
        return this.suppressedExceptions;
    }
}

