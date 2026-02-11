package org.spongepowered.asm.mixin.struct;

import java.util.Locale;
import javax.tools.Diagnostic;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.include.com.google.common.base.Strings;

public class AnnotatedMethodInfo
implements IInjectionPointContext {
    private final IMixinContext context;
    protected final MethodNode method;
    protected final AnnotationNode annotation;
    protected final String annotationType;
    protected final String methodName;

    public AnnotatedMethodInfo(IMixinContext mixin, MethodNode method, AnnotationNode annotation) {
        this.context = mixin;
        this.method = method;
        this.annotation = annotation;
        this.annotationType = this.annotation != null ? "@" + Annotations.getSimpleName(this.annotation) : "Undecorated method";
        this.methodName = MethodNodeEx.getName(method);
    }

    @Override
    public final String getElementDescription() {
        return String.format("%s annotation on %s", this.annotationType, this.methodName);
    }

    @Override
    public String remap(String reference) {
        if (this.context != null) {
            IReferenceMapper referenceMapper = this.context.getReferenceMapper();
            return referenceMapper != null ? referenceMapper.remap(this.context.getClassRef(), reference) : reference;
        }
        return reference;
    }

    @Override
    public ISelectorContext getParent() {
        return null;
    }

    @Override
    public final IMixinContext getMixin() {
        return this.context;
    }

    @Override
    public final MethodNode getMethod() {
        return this.method;
    }

    public String getMethodName() {
        return this.method.name;
    }

    @Override
    public AnnotationNode getAnnotationNode() {
        return this.annotation;
    }

    @Override
    public final IAnnotationHandle getAnnotation() {
        return Annotations.handleOf(this.annotation);
    }

    @Override
    public IAnnotationHandle getSelectorAnnotation() {
        return Annotations.handleOf(this.annotation);
    }

    @Override
    public String getSelectorCoordinate(boolean leaf) {
        return leaf ? "method" : this.getMethodName().toLowerCase(Locale.ROOT);
    }

    @Override
    public void addMessage(String format, Object ... args) {
        if (this.context.getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
            MessageRouter.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format(format, args));
        }
    }

    public static final String getDynamicInfo(Object method) {
        if (method instanceof MethodNode) {
            return AnnotatedMethodInfo.getDynamicInfo((MethodNode)method);
        }
        if (method instanceof IAnnotatedElement) {
            return AnnotatedMethodInfo.getDynamicInfo((IAnnotatedElement)method);
        }
        return "";
    }

    public static final String getDynamicInfo(MethodNode method) {
        return AnnotatedMethodInfo.getDynamicInfo(Annotations.handleOf(Annotations.getInvisible(method, Dynamic.class)));
    }

    public static final String getDynamicInfo(IAnnotatedElement method) {
        return AnnotatedMethodInfo.getDynamicInfo(method.getAnnotation(Dynamic.class));
    }

    private static String getDynamicInfo(IAnnotationHandle annotation) {
        if (annotation == null) {
            return "";
        }
        String description = Strings.nullToEmpty((String)annotation.getValue());
        Type upstream = annotation.getTypeValue("mixin");
        if (upstream != null) {
            description = String.format("{%s} %s", upstream.getClassName(), description).trim();
        }
        return description.length() > 0 ? String.format(" Method is @Dynamic(%s).", description) : "";
    }
}

