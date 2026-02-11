package org.spongepowered.tools.obfuscation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.SelectorAnnotationContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerAccessor;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerInjector;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerOverwrite;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerShadow;
import org.spongepowered.tools.obfuscation.AnnotatedMixinElementHandlerSoftImplements;
import org.spongepowered.tools.obfuscation.ObfuscationData;
import org.spongepowered.tools.obfuscation.SuppressedBy;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerSuppressible;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;

class AnnotatedMixin
implements IMixinContext,
IAnnotatedElement {
    private final IAnnotationHandle annotation;
    private final IMessagerSuppressible messager;
    private final ITypeHandleProvider typeProvider;
    private final IObfuscationManager obf;
    private final IMappingConsumer mappings;
    private final TypeElement mixin;
    private final List<MethodHandle> methods;
    private final TypeHandle handle;
    private final List<TypeHandle> targets = new ArrayList<TypeHandle>();
    private final TypeHandle primaryTarget;
    private final String classRef;
    private final boolean remap;
    private final boolean virtual;
    private final AnnotatedMixinElementHandlerOverwrite overwrites;
    private final AnnotatedMixinElementHandlerShadow shadows;
    private final AnnotatedMixinElementHandlerInjector injectors;
    private final AnnotatedMixinElementHandlerAccessor accessors;
    private final AnnotatedMixinElementHandlerSoftImplements softImplements;
    private boolean validated = false;

    public AnnotatedMixin(IMixinAnnotationProcessor ap, TypeElement type) {
        this.typeProvider = ap.getTypeProvider();
        this.obf = ap.getObfuscationManager();
        this.mappings = this.obf.createMappingConsumer();
        this.messager = ap;
        this.mixin = type;
        this.handle = new TypeHandle(type, ap.getTypeProvider());
        this.methods = new ArrayList<MethodHandle>(this.handle.getMethods());
        this.virtual = this.handle.getAnnotation(Pseudo.class).exists();
        this.annotation = this.handle.getAnnotation(Mixin.class);
        this.classRef = TypeUtils.getInternalName(type);
        this.primaryTarget = this.initTargets(ap);
        this.remap = this.annotation.getBoolean("remap", true) && this.targets.size() > 0;
        this.overwrites = new AnnotatedMixinElementHandlerOverwrite(ap, this);
        this.shadows = new AnnotatedMixinElementHandlerShadow(ap, this);
        this.injectors = new AnnotatedMixinElementHandlerInjector(ap, this);
        this.accessors = new AnnotatedMixinElementHandlerAccessor(ap, this);
        this.softImplements = new AnnotatedMixinElementHandlerSoftImplements(ap, this);
    }

    AnnotatedMixin runValidators(IMixinValidator.ValidationPass pass, Collection<IMixinValidator> validators) {
        for (IMixinValidator validator : validators) {
            if (!validator.validate(pass, this.mixin, this.annotation, this.targets)) break;
        }
        if (pass == IMixinValidator.ValidationPass.FINAL && !this.validated) {
            this.validated = true;
            this.runFinalValidation();
        }
        return this;
    }

    private TypeHandle initTargets(IMixinAnnotationProcessor ap) {
        TypeHandle type;
        TypeHandle primaryTarget = null;
        try {
            for (Object target : this.annotation.getList()) {
                type = this.typeProvider.getTypeHandle(target);
                if (type == null || this.targets.contains(type)) continue;
                this.addTarget(type);
                if (primaryTarget != null) continue;
                primaryTarget = type;
            }
        }
        catch (Exception ex) {
            this.printMessage(IMessagerEx.MessageType.WARNING, "Error processing public targets: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        try {
            for (String softTarget : this.annotation.getList("targets")) {
                type = this.typeProvider.getTypeHandle(softTarget);
                if (this.targets.contains(type)) continue;
                if (this.virtual) {
                    type = this.typeProvider.getSimulatedHandle(softTarget, this.mixin.asType());
                } else if (type == null) {
                    this.printMessage(IMessagerEx.MessageType.MIXIN_SOFT_TARGET_NOT_FOUND, "Mixin target " + softTarget + " could not be found");
                    if (IMessagerEx.MessageType.MIXIN_SOFT_TARGET_NOT_FOUND.isError()) {
                        return null;
                    }
                    type = this.typeProvider.getSimulatedHandle(softTarget, this.mixin.asType());
                } else if (type.isImaginary()) {
                    this.printMessage(IMessagerEx.MessageType.MIXIN_SOFT_TARGET_NOT_RESOLVED, "Mixin target " + softTarget + " could not be fully resolved.", SuppressedBy.UNRESOLVABLE_TARGET);
                    if (IMessagerEx.MessageType.MIXIN_SOFT_TARGET_NOT_RESOLVED.isError()) {
                        return null;
                    }
                } else if (type.isPublic()) {
                    SuppressedBy suppressedBy = type.getPackage().isUnnamed() ? SuppressedBy.DEFAULT_PACKAGE : SuppressedBy.PUBLIC_TARGET;
                    String must = IMessagerEx.MessageType.MIXIN_SOFT_TARGET_IS_PUBLIC.isError() ? "must" : "should";
                    this.printMessage(IMessagerEx.MessageType.MIXIN_SOFT_TARGET_IS_PUBLIC, "Mixin target " + softTarget + " is public and " + must + " be specified in value", suppressedBy);
                    if (IMessagerEx.MessageType.MIXIN_SOFT_TARGET_IS_PUBLIC.isError()) {
                        return null;
                    }
                }
                this.addSoftTarget(type, softTarget);
                if (primaryTarget != null) continue;
                primaryTarget = type;
            }
        }
        catch (Exception ex) {
            this.printMessage(IMessagerEx.MessageType.WARNING, "Error processing private targets: " + ex.getClass().getName() + ": " + ex.getMessage());
        }
        if (primaryTarget == null) {
            this.printMessage(IMessagerEx.MessageType.MIXIN_NO_TARGETS, "Mixin has no targets");
        }
        return primaryTarget;
    }

    private void printMessage(IMessagerEx.MessageType type, CharSequence msg) {
        this.messager.printMessage(type, msg, (Element)this.mixin, AnnotationHandle.asMirror(this.annotation));
    }

    private void printMessage(IMessagerEx.MessageType type, CharSequence msg, SuppressedBy suppressedBy) {
        this.messager.printMessage(type, msg, (Element)this.mixin, AnnotationHandle.asMirror(this.annotation), suppressedBy);
    }

    private void addSoftTarget(TypeHandle type, String reference) {
        ObfuscationData<String> obfClassData = this.obf.getDataProvider().getObfClass(type);
        if (!obfClassData.isEmpty()) {
            this.obf.getReferenceManager().addClassMapping(this.classRef, reference, obfClassData);
        }
        this.addTarget(type);
    }

    private void addTarget(TypeHandle type) {
        this.targets.add(type);
    }

    public String toString() {
        return this.mixin.getSimpleName().toString();
    }

    public TypeElement getMixinElement() {
        return this.mixin;
    }

    public TypeHandle getHandle() {
        return this.handle;
    }

    @Override
    public String getClassRef() {
        return this.classRef;
    }

    public List<TypeHandle> getTargets() {
        return this.targets;
    }

    public boolean isMultiTarget() {
        return this.targets.size() > 1;
    }

    public boolean remap() {
        return this.remap;
    }

    public IMappingConsumer getMappings() {
        return this.mappings;
    }

    private void runFinalValidation() {
        for (MethodHandle method : this.methods) {
            this.overwrites.registerMerge(method);
        }
    }

    private void removeMethod(ExecutableElement method) {
        MethodHandle handle = null;
        for (MethodHandle methodHandle : this.methods) {
            if (methodHandle.getElement() != method) continue;
            handle = methodHandle;
        }
        if (handle != null) {
            this.methods.remove(handle);
        }
    }

    public void registerOverwrite(ExecutableElement method, AnnotationHandle overwrite, boolean shouldRemap) {
        this.removeMethod(method);
        this.overwrites.registerOverwrite(new AnnotatedMixinElementHandlerOverwrite.AnnotatedElementOverwrite(method, overwrite, shouldRemap));
    }

    public void registerShadow(VariableElement field, AnnotationHandle shadow, boolean shouldRemap) {
        AnnotatedMixinElementHandlerShadow annotatedMixinElementHandlerShadow = this.shadows;
        annotatedMixinElementHandlerShadow.getClass();
        this.shadows.registerShadow(new AnnotatedMixinElementHandlerShadow.AnnotatedElementShadowField(annotatedMixinElementHandlerShadow, field, shadow, shouldRemap));
    }

    public void registerShadow(ExecutableElement method, AnnotationHandle shadow, boolean shouldRemap) {
        this.removeMethod(method);
        AnnotatedMixinElementHandlerShadow annotatedMixinElementHandlerShadow = this.shadows;
        annotatedMixinElementHandlerShadow.getClass();
        this.shadows.registerShadow(new AnnotatedMixinElementHandlerShadow.AnnotatedElementShadowMethod(annotatedMixinElementHandlerShadow, method, shadow, shouldRemap));
    }

    public void registerInjector(ExecutableElement method, AnnotationHandle inject, InjectorRemap remap) {
        this.removeMethod(method);
        AnnotatedMixinElementHandlerInjector.AnnotatedElementInjector injectorElement = new AnnotatedMixinElementHandlerInjector.AnnotatedElementInjector(method, inject, (IMixinContext)this, remap);
        this.injectors.registerInjector(injectorElement);
        List<IAnnotationHandle> ats = inject.getAnnotationList("at");
        for (IAnnotationHandle at : ats) {
            this.registerInjectionPoint(method, inject, "at", (AnnotationHandle)at, remap, "@At(%s)");
        }
        List<IAnnotationHandle> slices = inject.getAnnotationList("slice");
        for (IAnnotationHandle slice : slices) {
            IAnnotationHandle to;
            String id = slice.getValue("id", "");
            String coord = "slice";
            if (!Strings.isNullOrEmpty(id)) {
                coord = coord + "." + id;
            }
            SelectorAnnotationContext sliceContext = new SelectorAnnotationContext(injectorElement, slice, coord);
            IAnnotationHandle from = slice.getAnnotation("from");
            if (from != null) {
                this.registerSliceInjectionPoint(method, inject, "from", (AnnotationHandle)from, remap, "@Slice[" + id + "](from=@At(%s))", sliceContext);
            }
            if ((to = slice.getAnnotation("to")) == null) continue;
            this.registerSliceInjectionPoint(method, inject, "to", (AnnotationHandle)to, remap, "@Slice[" + id + "](to=@At(%s))", sliceContext);
        }
    }

    public void registerInjectionPoint(ExecutableElement element, AnnotationHandle inject, String selectorCoordinate, AnnotationHandle at, InjectorRemap remap, String format) {
        this.injectors.registerInjectionPoint(new AnnotatedMixinElementHandlerInjector.AnnotatedElementInjectionPoint(element, inject, this, selectorCoordinate, at, remap), format);
    }

    public void registerSliceInjectionPoint(ExecutableElement element, AnnotationHandle inject, String selectorCoordinate, AnnotationHandle at, InjectorRemap remap, String format, ISelectorContext parentContext) {
        this.injectors.registerInjectionPoint(new AnnotatedMixinElementHandlerInjector.AnnotatedElementSliceInjectionPoint(element, inject, this, selectorCoordinate, at, remap, parentContext), format);
    }

    public void registerAccessor(ExecutableElement element, AnnotationHandle accessor, boolean shouldRemap) {
        this.removeMethod(element);
        this.accessors.registerAccessor(new AnnotatedMixinElementHandlerAccessor.AnnotatedElementAccessor(element, accessor, (IMixinContext)this, shouldRemap));
    }

    public void registerInvoker(ExecutableElement element, AnnotationHandle invoker, boolean shouldRemap) {
        this.removeMethod(element);
        this.accessors.registerAccessor(new AnnotatedMixinElementHandlerAccessor.AnnotatedElementInvoker(element, invoker, (IMixinContext)this, shouldRemap));
    }

    public void registerSoftImplements(AnnotationHandle implementsAnnotation) {
        this.softImplements.process(implementsAnnotation);
    }

    @Override
    public ReferenceMapper getReferenceMapper() {
        return null;
    }

    @Override
    public String getClassName() {
        return this.getClassRef().replace('/', '.');
    }

    @Override
    public String getTargetClassName() {
        return this.primaryTarget.toString();
    }

    @Override
    public String getTargetClassRef() {
        return this.primaryTarget.getName();
    }

    @Override
    public IMixinInfo getMixin() {
        throw new UnsupportedOperationException("MixinInfo not available at compile time");
    }

    @Override
    public Extensions getExtensions() {
        throw new UnsupportedOperationException("Mixin Extensions not available at compile time");
    }

    @Override
    public boolean getOption(MixinEnvironment.Option option) {
        throw new UnsupportedOperationException("Options not available at compile time");
    }

    @Override
    public int getPriority() {
        throw new UnsupportedOperationException("Priority not available at compile time");
    }

    @Override
    public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
        return AnnotationHandle.of(this.mixin, annotationClass);
    }
}

