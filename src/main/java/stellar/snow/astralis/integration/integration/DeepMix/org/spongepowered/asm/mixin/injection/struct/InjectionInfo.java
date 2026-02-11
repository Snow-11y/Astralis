package org.spongepowered.asm.mixin.injection.struct;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IActivityContext;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.ISliceContext;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.code.MethodSlice;
import org.spongepowered.asm.mixin.injection.code.MethodSlices;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelectors;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorException;
import org.spongepowered.asm.mixin.injection.struct.CallbackInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;
import org.spongepowered.asm.mixin.injection.struct.ModifyArgInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.ModifyArgsInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.ModifyConstantInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.ModifyVariableInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.RedirectInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ActivityStack;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;

public abstract class InjectionInfo
extends SpecialMethodInfo
implements ISliceContext {
    private static Map<String, InjectorEntry> registry = new LinkedHashMap<String, InjectorEntry>();
    private static Class<? extends Annotation>[] registeredAnnotations = new Class[0];
    protected final ActivityStack activities = new ActivityStack(null);
    protected final boolean isStatic;
    protected final TargetSelectors targets;
    protected final MethodSlices slices;
    protected final String atKey;
    protected final List<AnnotationNode> injectionPointAnnotations = new ArrayList<AnnotationNode>();
    protected final List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
    protected final Map<Target, List<InjectionNodes.InjectionNode>> targetNodes = new LinkedHashMap<Target, List<InjectionNodes.InjectionNode>>();
    protected int targetCount = 0;
    protected Injector injector;
    protected InjectorGroupInfo group;
    private final List<MethodNode> injectedMethods = new ArrayList<MethodNode>(0);
    private int expectedCallbackCount = 1;
    private int requiredCallbackCount = 0;
    private int maxCallbackCount = Integer.MAX_VALUE;
    private int injectedCallbackCount = 0;
    private List<String> messages;
    private int order = 1000;

    protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        this(mixin, method, annotation, "at");
    }

    protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation, String atKey) {
        super(mixin, method, annotation);
        this.isStatic = Bytecode.isStatic(method);
        this.targets = new TargetSelectors(this, mixin.getTargetClassNode());
        this.slices = MethodSlices.parse(this);
        this.atKey = atKey;
        this.readAnnotation();
    }

    protected void readAnnotation() {
        if (this.annotation == null) {
            return;
        }
        this.activities.clear();
        try {
            this.targets.setPermissivePass(this.mixin.getOption(MixinEnvironment.Option.REFMAP_REMAP));
            IActivityContext.IActivity activity = this.activities.begin("Read Injection Points");
            this.readInjectionPoints();
            activity.next("Parse Requirements");
            this.parseRequirements();
            activity.next("Parse Order");
            this.parseOrder();
            activity.next("Parse Selectors");
            this.parseSelectors();
            activity.next("Find Targets");
            this.targets.find();
            activity.next("Validate Targets");
            this.targets.validate(this.expectedCallbackCount, this.requiredCallbackCount);
            activity.next("Parse Injection Points");
            this.parseInjectionPoints(this.injectionPointAnnotations);
            activity.next("Parse Injector");
            this.injector = this.parseInjector(this.annotation);
            activity.end();
        }
        catch (InvalidMixinException ex) {
            ex.prepend(this.activities);
            throw ex;
        }
        catch (Exception ex) {
            throw new InvalidMixinException(this.mixin, "Unexpected " + ex.getClass().getSimpleName() + " parsing " + this.getElementDescription(), (Throwable)ex, (IActivityContext)this.activities);
        }
    }

    protected void readInjectionPoints() {
        List ats = Annotations.getValue(this.annotation, this.atKey, false);
        if (ats == null) {
            throw new InvalidInjectionException((ISelectorContext)this, String.format("%s is missing '%s' value(s)", this.getElementDescription(), this.atKey));
        }
        this.injectionPointAnnotations.addAll(ats);
    }

    protected void parseRequirements() {
        Integer require;
        this.group = this.mixin.getInjectorGroups().parseGroup(this.method, this.mixin.getDefaultInjectorGroup()).add(this);
        Integer expect = (Integer)Annotations.getValue(this.annotation, "expect");
        if (expect != null) {
            this.expectedCallbackCount = expect;
        }
        if ((require = (Integer)Annotations.getValue(this.annotation, "require")) != null && require > -1) {
            this.requiredCallbackCount = require;
        } else if (this.group.isDefault()) {
            this.requiredCallbackCount = this.mixin.getDefaultRequiredInjections();
        }
        Integer allow = (Integer)Annotations.getValue(this.annotation, "allow");
        if (allow != null) {
            this.maxCallbackCount = Math.max(Math.max(this.requiredCallbackCount, 1), allow);
        }
    }

    protected void parseOrder() {
        Integer userOrder = (Integer)Annotations.getValue(this.annotation, "order");
        if (userOrder != null) {
            this.order = userOrder;
            return;
        }
        InjectorOrder injectorDefault = this.getClass().getAnnotation(InjectorOrder.class);
        this.order = injectorDefault != null ? injectorDefault.value() : 1000;
    }

    protected void parseSelectors() {
        LinkedHashSet<ITargetSelector> selectors = new LinkedHashSet<ITargetSelector>();
        TargetSelector.parse(Annotations.getValue(this.annotation, "method", false), this, selectors);
        TargetSelector.parse(Annotations.getValue(this.annotation, "target", false), this, selectors);
        if (selectors.size() == 0) {
            throw new InvalidInjectionException((ISelectorContext)this, String.format("%s is missing 'method' or 'target' to specify targets", this.getElementDescription()));
        }
        this.targets.parse(selectors);
    }

    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        this.injectionPoints.addAll(InjectionPoint.parse((IInjectionPointContext)this, ats));
    }

    protected abstract Injector parseInjector(AnnotationNode var1);

    public boolean isValid() {
        return this.targets.size() > 0 && this.injectionPoints.size() > 0;
    }

    public int getOrder() {
        return this.order;
    }

    public void prepare() {
        this.activities.clear();
        try {
            this.targetNodes.clear();
            IActivityContext.IActivity activity = this.activities.begin("?");
            for (TargetSelectors.SelectedMethod targetMethod : this.targets) {
                activity.next("{ target: %s }", targetMethod);
                Target target = this.mixin.getTargetMethod(targetMethod.getMethod());
                InjectorTarget injectorTarget = new InjectorTarget(this, target, targetMethod);
                try {
                    this.targetNodes.put(target, this.injector.find(injectorTarget, this.injectionPoints));
                }
                catch (SelectorException ex) {
                    throw new InvalidInjectionException((ISelectorContext)this, String.format("Injection validation failed: %s: %s. %s%s", this.getElementDescription(), ex.getMessage(), this.mixin.getReferenceMapper().getStatus(), AnnotatedMethodInfo.getDynamicInfo(this.method)));
                }
                finally {
                    injectorTarget.dispose();
                }
            }
            activity.end();
        }
        catch (InvalidMixinException ex) {
            ex.prepend(this.activities);
            throw ex;
        }
        catch (Exception ex) {
            throw new InvalidMixinException(this.mixin, "Unexpecteded " + ex.getClass().getSimpleName() + " preparing " + this.getElementDescription(), (Throwable)ex, (IActivityContext)this.activities);
        }
    }

    public void preInject() {
        for (Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : this.targetNodes.entrySet()) {
            this.injector.preInject(entry.getKey(), entry.getValue());
        }
    }

    public void inject() {
        for (Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : this.targetNodes.entrySet()) {
            this.injector.inject(entry.getKey(), entry.getValue());
        }
        this.targets.clear();
    }

    public void postInject() {
        for (MethodNode method : this.injectedMethods) {
            this.classNode.methods.add(method);
        }
        String description = this.getDescription();
        String refMapStatus = this.mixin.getReferenceMapper().getStatus();
        String extraInfo = AnnotatedMethodInfo.getDynamicInfo(this.method) + this.getMessages();
        if (this.mixin.getOption(MixinEnvironment.Option.DEBUG_INJECTORS) && this.injectedCallbackCount < this.expectedCallbackCount) {
            throw new InvalidInjectionException((ISelectorContext)this, String.format("Injection validation failed: %s %s%s in %s expected %d invocation(s) but %d succeeded. Scanned %d target(s). %s%s", description, this.methodName, this.method.desc, this.mixin, this.expectedCallbackCount, this.injectedCallbackCount, this.targetCount, refMapStatus, extraInfo));
        }
        if (this.injectedCallbackCount < this.requiredCallbackCount) {
            throw new InjectionError(String.format("Critical injection failure: %s %s%s in %s failed injection check, (%d/%d) succeeded. Scanned %d target(s). %s%s", description, this.methodName, this.method.desc, this.mixin, this.injectedCallbackCount, this.requiredCallbackCount, this.targetCount, refMapStatus, extraInfo));
        }
        if (this.injectedCallbackCount > this.maxCallbackCount) {
            throw new InjectionError(String.format("Critical injection failure: %s %s%s in %s failed injection check, %d succeeded of %d allowed.%s", description, this.methodName, this.method.desc, this.mixin, this.injectedCallbackCount, this.maxCallbackCount, extraInfo));
        }
        this.slices.postInject();
    }

    public void notifyInjected(Target target) {
    }

    protected String getDescription() {
        return "Callback method";
    }

    public String toString() {
        return InjectionInfo.describeInjector(this.mixin, this.annotation, this.method);
    }

    public int getTargetCount() {
        return this.targets.size();
    }

    @Override
    public MethodSlice getSlice(String id) {
        return this.slices.get(this.getSliceId(id));
    }

    public String getSliceId(String id) {
        return "";
    }

    public int getInjectedCallbackCount() {
        return this.injectedCallbackCount;
    }

    public MethodNode addMethod(int access, String name, String desc) {
        MethodNode method = new MethodNode(ASM.API_VERSION, access | 0x1000, name, desc, null, null);
        this.injectedMethods.add(method);
        return method;
    }

    public void addCallbackInvocation(MethodNode handler) {
        ++this.injectedCallbackCount;
    }

    @Override
    public void addMessage(String format, Object ... args) {
        super.addMessage(format, args);
        if (this.messages == null) {
            this.messages = new ArrayList<String>();
        }
        String message = String.format(format, args);
        this.messages.add(message);
    }

    protected String getMessages() {
        return this.messages != null ? " Messages: { " + Joiner.on(" ").join(this.messages) + "}" : "";
    }

    public static InjectionInfo parse(MixinTargetContext mixin, MethodNode method) {
        AnnotationNode annotation = InjectionInfo.getInjectorAnnotation(mixin.getMixin(), method);
        if (annotation == null) {
            return null;
        }
        for (InjectorEntry injector : registry.values()) {
            if (!annotation.desc.equals(injector.annotationDesc)) continue;
            return injector.create(mixin, method, annotation);
        }
        return null;
    }

    public static AnnotationNode getInjectorAnnotation(IMixinInfo mixin, MethodNode method) {
        AnnotationNode annotation = null;
        try {
            annotation = Annotations.getSingleVisible(method, registeredAnnotations);
        }
        catch (IllegalArgumentException ex) {
            throw new InvalidMixinException(mixin, String.format("Error parsing annotations on %s in %s: %s", method.name, mixin.getClassName(), ex.getMessage()));
        }
        return annotation;
    }

    public static String getInjectorPrefix(AnnotationNode annotation) {
        if (annotation == null) {
            return "handler";
        }
        for (InjectorEntry injector : registry.values()) {
            if (!annotation.desc.endsWith(injector.annotationDesc)) continue;
            return injector.prefix;
        }
        return "handler";
    }

    static String describeInjector(IMixinContext mixin, AnnotationNode annotation, MethodNode method) {
        return String.format("%s->@%s::%s%s", mixin.toString(), Annotations.getSimpleName(annotation), MethodNodeEx.getName(method), method.desc);
    }

    public static void register(Class<? extends InjectionInfo> type) {
        InjectorEntry entry;
        AnnotationType annotationType = type.getAnnotation(AnnotationType.class);
        if (annotationType == null) {
            throw new IllegalArgumentException("Injection info class " + type + " is not annotated with @AnnotationType");
        }
        try {
            entry = new InjectorEntry(annotationType.value(), type);
        }
        catch (NoSuchMethodException ex) {
            throw new MixinError("InjectionInfo class " + type.getName() + " is missing a valid constructor");
        }
        InjectorEntry existing = registry.get(entry.annotationDesc);
        if (existing != null) {
            MessageRouter.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format("Overriding InjectionInfo for @%s with %s (previously %s)", annotationType.value().getSimpleName(), type.getName(), existing.injectorType.getName()));
        } else {
            MessageRouter.getMessager().printMessage(Diagnostic.Kind.OTHER, String.format("Registering new injector for @%s with %s", annotationType.value().getSimpleName(), type.getName()));
        }
        registry.put(entry.annotationDesc, entry);
        ArrayList<Class<? extends Annotation>> annotations = new ArrayList<Class<? extends Annotation>>();
        for (InjectorEntry injector : registry.values()) {
            annotations.add(injector.annotationType);
        }
        registeredAnnotations = annotations.toArray(registeredAnnotations);
    }

    public static Set<Class<? extends Annotation>> getRegisteredAnnotations() {
        return ImmutableSet.copyOf(registeredAnnotations);
    }

    static {
        InjectionInfo.register(CallbackInjectionInfo.class);
        InjectionInfo.register(ModifyArgInjectionInfo.class);
        InjectionInfo.register(ModifyArgsInjectionInfo.class);
        InjectionInfo.register(RedirectInjectionInfo.class);
        InjectionInfo.register(ModifyVariableInjectionInfo.class);
        InjectionInfo.register(ModifyConstantInjectionInfo.class);
    }

    static class InjectorEntry {
        final Class<? extends Annotation> annotationType;
        final Class<? extends InjectionInfo> injectorType;
        final Constructor<? extends InjectionInfo> ctor;
        final String annotationDesc;
        final String prefix;

        InjectorEntry(Class<? extends Annotation> annotationType, Class<? extends InjectionInfo> type) throws NoSuchMethodException {
            this.annotationType = annotationType;
            this.injectorType = type;
            this.ctor = type.getDeclaredConstructor(MixinTargetContext.class, MethodNode.class, AnnotationNode.class);
            this.annotationDesc = Type.getDescriptor(annotationType);
            HandlerPrefix handlerPrefix = type.getAnnotation(HandlerPrefix.class);
            this.prefix = handlerPrefix != null ? handlerPrefix.value() : "handler";
        }

        InjectionInfo create(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
            try {
                return this.ctor.newInstance(mixin, method, annotation);
            }
            catch (InvocationTargetException itex) {
                Throwable cause = itex.getCause();
                if (cause instanceof MixinException) {
                    throw (MixinException)cause;
                }
                Throwable ex = cause != null ? cause : itex;
                throw new MixinError("Error initialising injector metaclass [" + this.injectorType + "] for annotation " + annotation.desc, ex);
            }
            catch (ReflectiveOperationException ex) {
                throw new MixinError("Failed to instantiate injector metaclass [" + this.injectorType + "] for annotation " + annotation.desc, ex);
            }
        }
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(value={ElementType.TYPE})
    public static @interface InjectorOrder {
        public static final int EARLY = 0;
        public static final int DEFAULT = 1000;
        public static final int LATE = 2000;
        public static final int REDIRECT = 10000;
        public static final int AFTER_REDIRECT = 20000;

        public int value() default 1000;
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(value={ElementType.TYPE})
    public static @interface HandlerPrefix {
        public static final String DEFAULT = "handler";

        public String value();
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(value={ElementType.TYPE})
    public static @interface AnnotationType {
        public Class<? extends Annotation> value();
    }
}

