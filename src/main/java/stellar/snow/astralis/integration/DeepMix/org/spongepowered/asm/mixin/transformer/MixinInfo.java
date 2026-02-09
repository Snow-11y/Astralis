package org.spongepowered.asm.mixin.transformer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.ModUtil;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.InterfaceInfo;
import org.spongepowered.asm.mixin.transformer.MixinConfig;
import org.spongepowered.asm.mixin.transformer.MixinPreProcessorAccessor;
import org.spongepowered.asm.mixin.transformer.MixinPreProcessorInterface;
import org.spongepowered.asm.mixin.transformer.MixinPreProcessorStandard;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.PluginHandle;
import org.spongepowered.asm.mixin.transformer.TargetClassContext;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinReloadException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTargetAlreadyLoadedException;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.LanguageFeatures;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.include.com.google.common.base.Functions;
import org.spongepowered.include.com.google.common.collect.Iterables;
import org.spongepowered.include.com.google.common.collect.Lists;

class MixinInfo
implements Comparable<MixinInfo>,
IMixinInfo {
    static int mixinOrder = 0;
    private final transient ILogger logger = MixinService.getService().getLogger("mixin");
    private final transient Profiler profiler = Profiler.getProfiler("mixin");
    private final transient MixinConfig parent;
    private final String name;
    private final String className;
    private final int priority;
    private final boolean virtual;
    private final transient List<DeclaredTarget> declaredTargets;
    private final transient List<ClassInfo> targetClasses = new ArrayList<ClassInfo>();
    private final List<String> targetClassNames = new ArrayList<String>();
    private final transient int order = mixinOrder++;
    private final transient IMixinService service;
    private final transient PluginHandle plugin;
    private final transient MixinEnvironment.Phase phase;
    private final transient ClassInfo info;
    private final transient SubType type;
    private final transient boolean strict;
    private final transient Extensions extensions;
    private transient State pendingState;
    private transient State state;

    MixinInfo(IMixinService service, MixinConfig parent, String name, PluginHandle plugin, boolean ignorePlugin, Extensions extensions) {
        IClassTracker tracker;
        this.service = service;
        this.parent = parent;
        this.name = name;
        this.className = parent.getMixinPackage() + name;
        this.plugin = plugin;
        this.phase = parent.getEnvironment().getPhase();
        this.strict = parent.getEnvironment().getOption(MixinEnvironment.Option.DEBUG_TARGETS);
        this.extensions = extensions;
        try {
            ClassNode mixinClassNode = this.loadMixinClass(this.className);
            this.pendingState = new State(mixinClassNode);
            this.info = this.pendingState.getClassInfo();
            this.type = SubType.getTypeFor(this);
        }
        catch (InvalidMixinException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new InvalidMixinException((IMixinInfo)this, ex.getMessage(), (Throwable)ex);
        }
        if (!this.type.isLoadable() && (tracker = this.service.getClassTracker()) != null) {
            tracker.registerInvalidClass(this.className);
        }
        try {
            this.priority = this.readPriority(this.pendingState.getClassNode());
            this.virtual = this.readPseudo(this.pendingState.getValidationClassNode());
            this.declaredTargets = this.readDeclaredTargets(this.pendingState.getValidationClassNode(), ignorePlugin);
        }
        catch (InvalidMixinException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new InvalidMixinException((IMixinInfo)this, (Throwable)ex);
        }
    }

    void parseTargets() {
        try {
            this.targetClasses.addAll(this.readTargetClasses(this.declaredTargets));
            this.targetClassNames.addAll(Lists.transform(this.targetClasses, Functions.toStringFunction()));
        }
        catch (InvalidMixinException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new InvalidMixinException((IMixinInfo)this, (Throwable)ex);
        }
    }

    void validate() {
        if (this.pendingState == null) {
            throw new IllegalStateException("No pending validation state for " + this);
        }
        try {
            this.pendingState.validate(this.type, this.targetClasses);
            this.state = this.pendingState;
        }
        finally {
            this.pendingState = null;
        }
    }

    protected List<DeclaredTarget> readDeclaredTargets(MixinClassNode classNode, boolean ignorePlugin) {
        if (classNode == null) {
            return Collections.emptyList();
        }
        AnnotationNode mixin = Annotations.getInvisible(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException((IMixinInfo)this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        IClassTracker tracker = this.service.getClassTracker();
        ArrayList<DeclaredTarget> declaredTargets = new ArrayList<DeclaredTarget>();
        for (Object target : this.readTargets(mixin)) {
            DeclaredTarget declaredTarget = DeclaredTarget.of(target, this);
            if (declaredTarget == null) continue;
            if (tracker != null && tracker.isClassLoaded(declaredTarget.name) && !this.isReloading()) {
                String message = String.format("Critical problem: %s target %s was loaded too early.", this, declaredTarget.name);
                if (this.parent.isRequired()) {
                    throw new MixinTargetAlreadyLoadedException((IMixinInfo)this, message, declaredTarget.name);
                }
                this.logger.error(message, new Object[0]);
            }
            if (!this.shouldApplyMixin(ignorePlugin, declaredTarget.name)) continue;
            declaredTargets.add(declaredTarget);
        }
        return declaredTargets;
    }

    private Iterable<Object> readTargets(AnnotationNode mixin) {
        Iterable publicTargets = (Iterable)Annotations.getValue(mixin, "value");
        Iterable privateTargets = (Iterable)Annotations.getValue(mixin, "targets");
        if (publicTargets == null && privateTargets == null) {
            return Collections.emptyList();
        }
        if (publicTargets == null) {
            return privateTargets;
        }
        return privateTargets == null ? publicTargets : Iterables.concat(publicTargets, privateTargets);
    }

    private boolean shouldApplyMixin(boolean ignorePlugin, String targetName) {
        Profiler.Section pluginTimer = this.profiler.begin("plugin");
        boolean result = ignorePlugin || this.plugin.shouldApplyMixin(targetName, this.className);
        pluginTimer.end();
        return result;
    }

    List<ClassInfo> readTargetClasses(MixinClassNode classNode, boolean ignorePlugin) {
        return this.readTargetClasses(this.readDeclaredTargets(classNode, ignorePlugin));
    }

    private List<ClassInfo> readTargetClasses(List<DeclaredTarget> declaredTargets) throws InvalidMixinException {
        ArrayList<ClassInfo> targetClasses = new ArrayList<ClassInfo>();
        for (DeclaredTarget target : declaredTargets) {
            ClassInfo targetClass = this.getTargetClass(target);
            if (targetClass == null) continue;
            targetClasses.add(targetClass);
            targetClass.addMixin(this);
        }
        return targetClasses;
    }

    private ClassInfo getTargetClass(DeclaredTarget target) throws InvalidMixinException {
        ClassInfo targetInfo = ClassInfo.forName(target.name);
        if (targetInfo == null) {
            if (this.isVirtual()) {
                this.logger.debug("Skipping virtual target {} for {}", target.name, this);
            } else {
                this.handleTargetError(String.format("@Mixin target %s was not found %s", target.name, this), false);
            }
            return null;
        }
        this.type.validateTarget(target.name, targetInfo);
        if (target.isPrivate && targetInfo.isReallyPublic() && !this.isVirtual()) {
            this.handleTargetError(String.format("@Mixin target %s is public in %s and should be specified in value", target.name, this), true);
        }
        return targetInfo;
    }

    private void handleTargetError(String message, boolean verboseOnly) {
        if (this.strict) {
            this.logger.error(message, new Object[0]);
            throw new InvalidMixinException((IMixinInfo)this, message);
        }
        this.logger.log(verboseOnly && !this.parent.isVerboseLogging() ? Level.DEBUG : Level.WARN, message, new Object[0]);
    }

    protected int readPriority(ClassNode classNode) {
        if (classNode == null) {
            return this.parent.getDefaultMixinPriority();
        }
        AnnotationNode mixin = Annotations.getInvisible(classNode, Mixin.class);
        if (mixin == null) {
            throw new InvalidMixinException((IMixinInfo)this, String.format("The mixin '%s' is missing an @Mixin annotation", this.className));
        }
        Integer priority = (Integer)Annotations.getValue(mixin, "priority");
        return priority == null ? this.parent.getDefaultMixinPriority() : priority.intValue();
    }

    protected boolean readPseudo(ClassNode classNode) {
        return Annotations.getInvisible(classNode, Pseudo.class) != null;
    }

    private boolean isReloading() {
        return this.pendingState instanceof Reloaded;
    }

    String remapClassName(String className) {
        return this.parent.remapClassName(this.getClassRef(), className);
    }

    public boolean hasDeclaredTarget(String targetClass) {
        for (DeclaredTarget declaredTarget : this.declaredTargets) {
            if (!targetClass.equals(declaredTarget.name)) continue;
            return true;
        }
        return false;
    }

    private State getState() {
        return this.state != null ? this.state : this.pendingState;
    }

    ClassInfo getClassInfo() {
        return this.info;
    }

    @Override
    public IMixinConfig getConfig() {
        return this.parent;
    }

    MixinConfig getParent() {
        return this.parent;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getClassRef() {
        return this.getClassInfo().getName();
    }

    @Override
    public byte[] getClassBytes() {
        throw new RuntimeException("NO");
    }

    @Override
    public boolean isDetachedSuper() {
        return this.getState().isDetachedSuper();
    }

    public boolean isUnique() {
        return this.getState().isUnique();
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    public boolean isAccessor() {
        return this.type instanceof SubType.Accessor;
    }

    public boolean isLoadable() {
        return this.type.isLoadable();
    }

    public boolean isRequired() {
        return this.parent.isRequired();
    }

    public Level getLoggingLevel() {
        return this.parent.getLoggingLevel();
    }

    @Override
    public MixinEnvironment.Phase getPhase() {
        return this.phase;
    }

    @Override
    public MixinClassNode getClassNode(int flags) {
        return this.getState().createClassNode(flags);
    }

    List<String> getDeclaredTargetClasses() {
        return Collections.unmodifiableList(Lists.transform(this.declaredTargets, Functions.toStringFunction()));
    }

    @Override
    public List<String> getTargetClasses() {
        return Collections.unmodifiableList(this.targetClassNames);
    }

    List<InterfaceInfo> getSoftImplements() {
        return Collections.unmodifiableList(this.getState().getSoftImplements());
    }

    Set<String> getSyntheticInnerClasses() {
        return Collections.unmodifiableSet(this.getState().getSyntheticInnerClasses());
    }

    Set<String> getInnerClasses() {
        return Collections.unmodifiableSet(this.getState().getInnerClasses());
    }

    List<ClassInfo> getTargets() {
        return Collections.unmodifiableList(this.targetClasses);
    }

    Set<String> getInterfaces() {
        return this.getState().getInterfaces();
    }

    int getClassVersion() {
        return this.getState().getClassNode().version;
    }

    Extensions getExtensions() {
        return this.extensions;
    }

    MixinTargetContext createContextFor(TargetClassContext target) {
        MixinClassNode classNode = this.getClassNode(8);
        Profiler.Section preTimer = this.profiler.begin("pre");
        MixinTargetContext context = this.type.createPreProcessor(classNode).prepare(this.extensions).createContextFor(target);
        preTimer.end();
        return context;
    }

    private ClassNode loadMixinClass(String mixinClassName) throws ClassNotFoundException {
        ClassNode classNode = null;
        try {
            String restrictions;
            IClassTracker tracker = this.service.getClassTracker();
            if (tracker != null && (restrictions = tracker.getClassRestrictions(mixinClassName)).length() > 0) {
                this.logger.error("Classloader restrictions [{}] encountered loading {}, name: {}", restrictions, this, mixinClassName);
            }
            int readerFlags = this.parent.getEnvironment().getOption(MixinEnvironment.Option.CLASSREADER_EXPAND_FRAMES) ? 8 : 0;
            classNode = this.service.getBytecodeProvider().getClassNode(mixinClassName, true, readerFlags);
        }
        catch (ClassNotFoundException ex) {
            throw new ClassNotFoundException(String.format("The specified mixin '%s' was not found", mixinClassName));
        }
        catch (IOException ex) {
            this.logger.warn("Failed to load mixin {}, the specified mixin will not be applied", mixinClassName);
            throw new InvalidMixinException((IMixinInfo)this, "An error was encountered whilst loading the mixin class", (Throwable)ex);
        }
        return classNode;
    }

    void reloadMixin(ClassNode classNode) {
        if (this.pendingState != null) {
            throw new IllegalStateException("Cannot reload mixin while it is initialising");
        }
        this.pendingState = new Reloaded(this.state, classNode);
        this.validate();
    }

    @Override
    public int compareTo(MixinInfo other) {
        if (other == null) {
            return 0;
        }
        if (other.priority == this.priority) {
            return Integer.compare(this.order, other.order);
        }
        return this.priority < other.priority ? -1 : 1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void preApply(String transformedName, ClassNode targetClass) throws Exception {
        if (this.plugin.isAvailable()) {
            Profiler.Section pluginTimer = this.profiler.begin("plugin");
            try {
                this.plugin.preApply(transformedName, targetClass, this.className, this);
            }
            finally {
                pluginTimer.end();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void postApply(String transformedName, ClassNode targetClass) throws Exception {
        if (this.plugin.isAvailable()) {
            Profiler.Section pluginTimer = this.profiler.begin("plugin");
            try {
                this.plugin.postApply(transformedName, targetClass, this.className, this);
            }
            finally {
                pluginTimer.end();
            }
        }
        this.parent.postApply(transformedName, targetClass);
        this.info.addAppliedMixin(this);
    }

    public String toString() {
        return String.format("%s:%s from owner %s", this.parent.getName(), this.name, ModUtil.owner(this.getConfig()));
    }

    static Variant getVariant(ClassNode classNode) {
        return MixinInfo.getVariant(ClassInfo.fromClassNode(classNode));
    }

    static Variant getVariant(ClassInfo classInfo) {
        if (!classInfo.isInterface()) {
            return Variant.STANDARD;
        }
        boolean containsNonAccessorMethod = false;
        for (ClassInfo.Method method : classInfo.getMethods()) {
            containsNonAccessorMethod |= !method.isAccessor() && !method.isSynthetic();
        }
        if (containsNonAccessorMethod) {
            return Variant.INTERFACE;
        }
        return Variant.ACCESSOR;
    }

    static final class DeclaredTarget {
        final String name;
        final boolean isPrivate;

        private DeclaredTarget(String name, boolean isPrivate) {
            this.name = name;
            this.isPrivate = isPrivate;
        }

        public String toString() {
            return this.name;
        }

        static DeclaredTarget of(Object target, MixinInfo info) {
            if (target instanceof String) {
                String remappedName = info.remapClassName((String)target);
                return remappedName != null ? new DeclaredTarget(remappedName, true) : null;
            }
            if (target instanceof Type) {
                return new DeclaredTarget(((Type)target).getClassName(), false);
            }
            return null;
        }
    }

    static abstract class SubType {
        protected final MixinInfo mixin;
        protected final String annotationType;
        protected final boolean targetMustBeInterface;
        protected boolean detached;

        SubType(MixinInfo info, String annotationType, boolean targetMustBeInterface) {
            this.mixin = info;
            this.annotationType = annotationType;
            this.targetMustBeInterface = targetMustBeInterface;
        }

        Collection<String> getInterfaces() {
            return Collections.emptyList();
        }

        boolean isDetachedSuper() {
            return this.detached;
        }

        boolean isLoadable() {
            return false;
        }

        void validateTarget(String targetName, ClassInfo targetInfo) {
            boolean targetIsInterface = targetInfo.isInterface();
            if (targetIsInterface != this.targetMustBeInterface) {
                String not = targetIsInterface ? "" : "not ";
                throw new InvalidMixinException((IMixinInfo)this.mixin, this.annotationType + " target type mismatch: " + targetName + " is " + not + "an interface in " + this);
            }
        }

        abstract void validate(State var1, List<ClassInfo> var2);

        abstract MixinPreProcessorStandard createPreProcessor(MixinClassNode var1);

        static SubType getTypeFor(MixinInfo mixin) {
            Variant variant = MixinInfo.getVariant(mixin.getClassInfo());
            switch (variant) {
                case STANDARD: {
                    return new Standard(mixin);
                }
                case INTERFACE: {
                    return new Interface(mixin);
                }
                case ACCESSOR: {
                    return new Accessor(mixin);
                }
            }
            throw new IllegalStateException("Unsupported Mixin variant " + (Object)((Object)variant) + " for " + mixin);
        }

        static class Accessor
        extends SubType {
            private final Collection<String> interfaces = new ArrayList<String>();

            Accessor(MixinInfo info) {
                super(info, "@Mixin", false);
                this.interfaces.add(info.getClassRef());
            }

            @Override
            boolean isLoadable() {
                return true;
            }

            @Override
            Collection<String> getInterfaces() {
                return this.interfaces;
            }

            @Override
            void validateTarget(String targetName, ClassInfo targetInfo) {
                boolean targetIsInterface = targetInfo.isInterface();
                if (targetIsInterface && !MixinEnvironment.getCompatibilityLevel().supports(1)) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, "Accessor mixin targetting an interface is not supported in current enviromnment");
                }
            }

            @Override
            void validate(State state, List<ClassInfo> targetClasses) {
                MixinClassNode classNode = state.getValidationClassNode();
                if (!"java/lang/Object".equals(classNode.superName)) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, "Super class of " + this + " is invalid, found " + classNode.superName.replace('/', '.'));
                }
            }

            @Override
            MixinPreProcessorStandard createPreProcessor(MixinClassNode classNode) {
                return new MixinPreProcessorAccessor(this.mixin, classNode);
            }
        }

        static class Interface
        extends SubType {
            Interface(MixinInfo info) {
                super(info, "@Mixin", true);
            }

            @Override
            void validate(State state, List<ClassInfo> targetClasses) {
                if (!MixinEnvironment.getCompatibilityLevel().supports(1)) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, "Interface mixin not supported in current enviromnment");
                }
                MixinClassNode classNode = state.getValidationClassNode();
                if (!"java/lang/Object".equals(classNode.superName)) {
                    throw new InvalidMixinException((IMixinInfo)this.mixin, "Super class of " + this + " is invalid, found " + classNode.superName.replace('/', '.'));
                }
            }

            @Override
            MixinPreProcessorStandard createPreProcessor(MixinClassNode classNode) {
                return new MixinPreProcessorInterface(this.mixin, classNode);
            }
        }

        static class Standard
        extends SubType {
            Standard(MixinInfo info) {
                super(info, "@Mixin", false);
            }

            @Override
            void validate(State state, List<ClassInfo> targetClasses) {
                MixinClassNode classNode = state.getValidationClassNode();
                for (ClassInfo targetClass : targetClasses) {
                    if (classNode.superName.equals(targetClass.getSuperName())) continue;
                    if (!targetClass.hasSuperClass(classNode.superName, ClassInfo.Traversal.SUPER)) {
                        ClassInfo superClass = ClassInfo.forName(classNode.superName);
                        if (superClass != null && superClass.isMixin()) {
                            for (ClassInfo superTarget : superClass.getTargets()) {
                                if (!targetClasses.contains(superTarget)) continue;
                                throw new InvalidMixinException((IMixinInfo)this.mixin, "Illegal hierarchy detected. Derived mixin " + this + " targets the same class " + superTarget.getClassName() + " as its superclass " + superClass.getClassName());
                            }
                        }
                        throw new InvalidMixinException((IMixinInfo)this.mixin, "Super class '" + classNode.superName.replace('/', '.') + "' of " + this.mixin.getName() + " was not found in the hierarchy of target class '" + targetClass + "'");
                    }
                    this.detached = true;
                }
            }

            @Override
            MixinPreProcessorStandard createPreProcessor(MixinClassNode classNode) {
                return new MixinPreProcessorStandard(this.mixin, classNode);
            }
        }
    }

    class Reloaded
    extends State {
        private final State previous;

        Reloaded(State previous, ClassNode classNode) {
            super(classNode, previous.getClassInfo());
            this.previous = previous;
        }

        @Override
        protected void validateChanges(SubType type, List<ClassInfo> targetClasses) {
            if (!this.syntheticInnerClasses.equals(this.previous.syntheticInnerClasses)) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change inner classes");
            }
            if (!this.interfaces.equals(this.previous.interfaces)) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change interfaces");
            }
            if (!new HashSet(this.softImplements).equals(new HashSet<InterfaceInfo>(this.previous.softImplements))) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change soft interfaces");
            }
            List<ClassInfo> targets = MixinInfo.this.readTargetClasses(this.validationClassNode, true);
            if (!new HashSet<ClassInfo>(targets).equals(new HashSet<ClassInfo>(targetClasses))) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change target classes");
            }
            int priority = MixinInfo.this.readPriority(this.validationClassNode);
            if (priority != MixinInfo.this.getPriority()) {
                throw new MixinReloadException(MixinInfo.this, "Cannot change mixin priority");
            }
        }
    }

    class State {
        private final ClassNode classNode;
        private final ClassInfo classInfo;
        private boolean detachedSuper;
        private boolean unique;
        protected final Set<String> interfaces = new HashSet<String>();
        protected final List<InterfaceInfo> softImplements = new ArrayList<InterfaceInfo>();
        protected final Set<String> syntheticInnerClasses = new HashSet<String>();
        protected final Set<String> innerClasses = new HashSet<String>();
        protected MixinClassNode validationClassNode;

        State(ClassNode classNode) {
            this(classNode, null);
        }

        State(ClassNode classNode, ClassInfo classInfo) {
            this.classNode = classNode;
            this.connect();
            this.classInfo = classInfo != null ? classInfo : ClassInfo.fromClassNode(this.getValidationClassNode());
        }

        protected void connect() {
            this.validationClassNode = this.createClassNode(0);
        }

        protected void complete() {
            this.validationClassNode = null;
        }

        ClassInfo getClassInfo() {
            return this.classInfo;
        }

        ClassNode getClassNode() {
            return this.classNode;
        }

        MixinClassNode getValidationClassNode() {
            if (this.validationClassNode == null) {
                throw new IllegalStateException("Attempted a validation task after validation is complete on " + this + " in " + MixinInfo.this);
            }
            return this.validationClassNode;
        }

        boolean isDetachedSuper() {
            return this.detachedSuper;
        }

        boolean isUnique() {
            return this.unique;
        }

        List<? extends InterfaceInfo> getSoftImplements() {
            return this.softImplements;
        }

        Set<String> getSyntheticInnerClasses() {
            return this.syntheticInnerClasses;
        }

        Set<String> getInnerClasses() {
            return this.innerClasses;
        }

        Set<String> getInterfaces() {
            return this.interfaces;
        }

        MixinClassNode createClassNode(int flags) {
            MixinClassNode mixinClassNode = new MixinClassNode(MixinInfo.this);
            this.classNode.accept((ClassVisitor)mixinClassNode);
            return mixinClassNode;
        }

        void validate(SubType type, List<ClassInfo> targetClasses) {
            MixinClassNode classNode = this.getValidationClassNode();
            MixinPreProcessorStandard preProcessor = type.createPreProcessor(classNode).prepare(MixinInfo.this.getExtensions());
            for (ClassInfo target : targetClasses) {
                preProcessor.conform(target);
            }
            type.validate(this, targetClasses);
            this.detachedSuper = type.isDetachedSuper();
            this.unique = Annotations.getVisible(classNode, Unique.class) != null;
            this.validateInner();
            this.validateClassFeatures();
            this.validateRemappables(targetClasses);
            this.readImplementations(type);
            this.readInnerClasses();
            this.validateChanges(type, targetClasses);
            this.complete();
        }

        private void validateInner() {
            if (!this.classInfo.isProbablyStatic()) {
                throw new InvalidMixinException((IMixinInfo)MixinInfo.this, "Inner class mixin must be declared static");
            }
        }

        private void validateClassFeatures() {
            MixinEnvironment.CompatibilityLevel compatibilityLevel = MixinEnvironment.getCompatibilityLevel();
            int requiredLanguageFeatures = LanguageFeatures.scan(this.validationClassNode);
            if (requiredLanguageFeatures == 0 || compatibilityLevel.supports(requiredLanguageFeatures)) {
                return;
            }
            int missingFeatures = requiredLanguageFeatures & ~compatibilityLevel.getLanguageFeatures();
            MixinEnvironment.CompatibilityLevel minRequiredLevel = MixinEnvironment.CompatibilityLevel.requiredFor(requiredLanguageFeatures);
            throw new InvalidMixinException((IMixinInfo)MixinInfo.this, String.format("Unsupported mixin, %s requires the following unsupported language features: %s, these features require compatibility level %s", MixinInfo.this, LanguageFeatures.format(missingFeatures), minRequiredLevel != null ? minRequiredLevel.toString() : "UNKNOWN"));
        }

        private void validateRemappables(List<ClassInfo> targetClasses) {
            if (targetClasses.size() > 1) {
                for (FieldNode field : this.validationClassNode.fields) {
                    this.validateRemappable(Shadow.class, field.name, Annotations.getVisible(field, Shadow.class));
                }
                for (MethodNode method : this.validationClassNode.methods) {
                    this.validateRemappable(Shadow.class, method.name, Annotations.getVisible(method, Shadow.class));
                    AnnotationNode overwrite = Annotations.getVisible(method, Overwrite.class);
                    if (overwrite == null || (method.access & 8) != 0 && (method.access & 1) != 0) continue;
                    throw new InvalidMixinException((IMixinInfo)MixinInfo.this, "Found @Overwrite annotation on " + method.name + " in " + MixinInfo.this);
                }
            }
        }

        private void validateRemappable(Class<Shadow> annotationClass, String name, AnnotationNode annotation) {
            if (annotation != null && Annotations.getValue(annotation, "remap", Boolean.TRUE).booleanValue()) {
                throw new InvalidMixinException((IMixinInfo)MixinInfo.this, "Found a remappable @" + annotationClass.getSimpleName() + " annotation on " + name + " in " + this);
            }
        }

        void readImplementations(SubType type) {
            this.interfaces.addAll(this.validationClassNode.interfaces);
            this.interfaces.addAll(type.getInterfaces());
            AnnotationNode implementsAnnotation = Annotations.getInvisible(this.validationClassNode, Implements.class);
            if (implementsAnnotation == null) {
                return;
            }
            List interfaces = (List)Annotations.getValue(implementsAnnotation);
            if (interfaces == null) {
                return;
            }
            for (AnnotationNode interfaceNode : interfaces) {
                InterfaceInfo interfaceInfo = InterfaceInfo.fromAnnotation(MixinInfo.this, interfaceNode);
                this.softImplements.add(interfaceInfo);
                this.interfaces.add(interfaceInfo.getInternalName());
                if (this instanceof Reloaded) continue;
                this.classInfo.addInterface(interfaceInfo.getInternalName());
            }
        }

        void readInnerClasses() {
            for (InnerClassNode inner : this.validationClassNode.innerClasses) {
                ClassInfo innerClass = ClassInfo.forName(inner.name);
                if ((inner.outerName == null || !inner.outerName.equals(this.classInfo.getName())) && !inner.name.startsWith(this.validationClassNode.name + "$")) continue;
                if (innerClass.isProbablyStatic() && innerClass.isSynthetic()) {
                    this.syntheticInnerClasses.add(inner.name);
                    continue;
                }
                if (innerClass.isMixin()) continue;
                this.innerClasses.add(inner.name);
            }
        }

        protected void validateChanges(SubType type, List<ClassInfo> targetClasses) {
            type.createPreProcessor(this.validationClassNode).prepare(MixinInfo.this.getExtensions());
        }
    }

    class MixinClassNode
    extends ClassNode {
        public final List<MixinMethodNode> mixinMethods;

        MixinClassNode(MixinInfo mixin) {
            this(ASM.API_VERSION);
        }

        protected MixinClassNode(int api) {
            super(api);
            this.mixinMethods = this.methods;
        }

        public MixinInfo getMixin() {
            return MixinInfo.this;
        }

        public List<FieldNode> getFields() {
            return new ArrayList<FieldNode>(this.fields);
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MixinMethodNode method = new MixinMethodNode(access, name, desc, signature, exceptions);
            this.methods.add(method);
            return method;
        }
    }

    class MixinMethodNode
    extends MethodNodeEx {
        public MixinMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
            super(access, name, desc, signature, exceptions, MixinInfo.this);
        }

        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object ... bootstrapMethodArguments) {
            Object[] bsmArgs = new Object[bootstrapMethodArguments.length];
            System.arraycopy(bootstrapMethodArguments, 0, bsmArgs, 0, bootstrapMethodArguments.length);
            this.instructions.add((AbstractInsnNode)new InvokeDynamicInsnNode(name, descriptor, bootstrapMethodHandle, bsmArgs));
        }

        public boolean isInjector() {
            return this.getInjectorAnnotation() != null || this.isSurrogate();
        }

        public boolean isSurrogate() {
            return this.getVisibleAnnotation(Surrogate.class) != null;
        }

        public boolean isSynthetic() {
            return Bytecode.hasFlag(this, 4096);
        }

        public AnnotationNode getVisibleAnnotation(Class<? extends Annotation> annotationClass) {
            return Annotations.getVisible(this, annotationClass);
        }

        public AnnotationNode getInjectorAnnotation() {
            return InjectionInfo.getInjectorAnnotation(MixinInfo.this, this);
        }
    }

    static enum Variant {
        STANDARD,
        INTERFACE,
        ACCESSOR,
        PROXY;

    }
}

