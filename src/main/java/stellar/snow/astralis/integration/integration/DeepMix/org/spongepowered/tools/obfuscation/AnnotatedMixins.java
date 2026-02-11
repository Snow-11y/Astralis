package org.spongepowered.tools.obfuscation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.asm.util.VersionNumber;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.tools.obfuscation.AnnotatedMixin;
import org.spongepowered.tools.obfuscation.ObfuscationManager;
import org.spongepowered.tools.obfuscation.SuppressedBy;
import org.spongepowered.tools.obfuscation.TargetMap;
import org.spongepowered.tools.obfuscation.interfaces.IJavadocProvider;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandleASM;
import org.spongepowered.tools.obfuscation.mirror.TypeHandleSimulated;
import org.spongepowered.tools.obfuscation.mirror.TypeReference;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;
import org.spongepowered.tools.obfuscation.validation.ParentValidator;
import org.spongepowered.tools.obfuscation.validation.TargetValidator;

final class AnnotatedMixins
implements ITokenProvider,
IJavadocProvider,
IMixinAnnotationProcessor,
ITypeHandleProvider {
    private static Map<ProcessingEnvironment, AnnotatedMixins> instances = new HashMap<ProcessingEnvironment, AnnotatedMixins>();
    private final IMixinAnnotationProcessor.CompilerEnvironment env;
    private final ProcessingEnvironment processingEnv;
    private final Map<String, AnnotatedMixin> mixins = new HashMap<String, AnnotatedMixin>();
    private final List<AnnotatedMixin> mixinsForPass = new ArrayList<AnnotatedMixin>();
    private final IObfuscationManager obf;
    private final List<IMixinValidator> validators;
    private final Map<String, Integer> tokenCache = new HashMap<String, Integer>();
    private final TargetMap targets;
    private Properties properties;

    private AnnotatedMixins(ProcessingEnvironment processingEnv) {
        this.env = IMixinAnnotationProcessor.CompilerEnvironment.detect(processingEnv);
        this.processingEnv = processingEnv;
        IMessagerEx.MessageType.applyOptions(this.env, this);
        MessageRouter.setMessager(processingEnv.getMessager());
        String pluginVersion = this.checkPluginVersion(this.getOption("pluginVersion"));
        String pluginVersionString = pluginVersion != null ? String.format(" (MixinGradle Version=%s)", pluginVersion) : "";
        this.printMessage(IMessagerEx.MessageType.INFO, (CharSequence)("SpongePowered MIXIN Annotation Processor Version=0.8.7" + pluginVersionString));
        this.targets = this.initTargetMap();
        this.obf = new ObfuscationManager(this);
        this.obf.init();
        this.validators = ImmutableList.of(new ParentValidator(this), new TargetValidator(this));
        this.initTokenCache(this.getOption("tokens"));
    }

    private String checkPluginVersion(String version) {
        VersionNumber recommendedVersion;
        if (version == null) {
            return null;
        }
        VersionNumber pluginVersion = VersionNumber.parse(version);
        if (pluginVersion.compareTo(recommendedVersion = VersionNumber.parse("0.7")) < 0) {
            this.printMessage(Diagnostic.Kind.WARNING, (CharSequence)String.format("MixinGradle version %s is out of date. Update to the recommended version %s", pluginVersion, recommendedVersion));
        }
        return pluginVersion.toString();
    }

    protected TargetMap initTargetMap() {
        TargetMap targets = TargetMap.create(System.getProperty("fabric.mixin.target.mapid"));
        System.setProperty("fabric.mixin.target.mapid", targets.getSessionId());
        String targetsFileName = this.getOption("dependencyTargetsFile");
        if (targetsFileName != null) {
            try {
                targets.readImports(new File(targetsFileName));
            }
            catch (IOException ex) {
                this.printMessage(Diagnostic.Kind.WARNING, (CharSequence)("Could not read from specified imports file: " + targetsFileName));
            }
        }
        return targets;
    }

    private void initTokenCache(String tokens) {
        if (tokens != null) {
            String[] tokenValues;
            Pattern tokenPattern = Pattern.compile("^([A-Z0-9\\-_\\.]+)=([0-9]+)$");
            for (String tokenValue : tokenValues = tokens.replaceAll("\\s", "").toUpperCase(Locale.ROOT).split("[;,]")) {
                Matcher tokenMatcher = tokenPattern.matcher(tokenValue);
                if (!tokenMatcher.matches()) continue;
                this.tokenCache.put(tokenMatcher.group(1), Integer.parseInt(tokenMatcher.group(2)));
            }
        }
    }

    @Override
    public ITypeHandleProvider getTypeProvider() {
        return this;
    }

    @Override
    public ITokenProvider getTokenProvider() {
        return this;
    }

    @Override
    public IObfuscationManager getObfuscationManager() {
        return this.obf;
    }

    @Override
    public IJavadocProvider getJavadocProvider() {
        return this;
    }

    @Override
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.processingEnv;
    }

    @Override
    public IMixinAnnotationProcessor.CompilerEnvironment getCompilerEnvironment() {
        return this.env;
    }

    @Override
    public Integer getToken(String token) {
        if (this.tokenCache.containsKey(token)) {
            return this.tokenCache.get(token);
        }
        String option = this.getOption(token);
        Integer value = null;
        try {
            value = Integer.parseInt(option);
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.tokenCache.put(token, value);
        return value;
    }

    @Override
    public String getOption(String option) {
        if (option == null) {
            return null;
        }
        String value = this.processingEnv.getOptions().get(option);
        if (value != null) {
            return value;
        }
        return this.getProperties().getProperty(option);
    }

    @Override
    public String getOption(String option, String defaultValue) {
        String value = this.getOption(option);
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean getOption(String option, boolean defaultValue) {
        String value = this.getOption(option);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    @Override
    public List<String> getOptions(String option) {
        ImmutableList.Builder list = ImmutableList.builder();
        String value = this.getOption(option);
        if (value != null) {
            for (String part : value.split(",")) {
                list.add(part);
            }
        }
        return list.build();
    }

    public Properties getProperties() {
        if (this.properties == null) {
            this.properties = new Properties();
            try {
                Filer filer = this.processingEnv.getFiler();
                FileObject propertyFile = filer.getResource(StandardLocation.SOURCE_PATH, "", "mixin.properties");
                if (propertyFile != null) {
                    InputStream inputStream = propertyFile.openInputStream();
                    this.properties.load(inputStream);
                    inputStream.close();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return this.properties;
    }

    public void writeMappings() {
        this.obf.writeMappings();
    }

    public void writeReferences() {
        this.obf.writeReferences();
    }

    public void registerMixin(TypeElement mixinType) {
        String name = mixinType.getQualifiedName().toString();
        if (!this.mixins.containsKey(name)) {
            AnnotatedMixin mixin = new AnnotatedMixin(this, mixinType);
            this.targets.registerTargets(mixin);
            mixin.runValidators(IMixinValidator.ValidationPass.EARLY, this.validators);
            this.mixins.put(name, mixin);
            this.mixinsForPass.add(mixin);
        }
    }

    public AnnotatedMixin getMixin(TypeElement mixinType) {
        return this.getMixin(mixinType.getQualifiedName().toString());
    }

    public AnnotatedMixin getMixin(String mixinType) {
        return this.mixins.get(mixinType);
    }

    public Collection<TypeHandle> getMixinsTargeting(TypeHandle targetType) {
        ArrayList<TypeHandle> minions = new ArrayList<TypeHandle>();
        for (TypeReference mixin : this.targets.getMixinsTargeting(targetType)) {
            TypeHandle handle = mixin.getHandle(this);
            if (handle == null) continue;
            minions.add(handle);
        }
        return minions;
    }

    public void registerAccessor(TypeElement mixinType, ExecutableElement method) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(IMessagerEx.MessageType.ACCESSOR_ON_NON_MIXIN_METHOD, (CharSequence)"Found @Accessor annotation on a non-mixin method", (Element)method);
            return;
        }
        AnnotationHandle accessor = AnnotationHandle.of(method, Accessor.class);
        mixinClass.registerAccessor(method, accessor, AnnotatedMixins.shouldRemap(mixinClass, accessor));
    }

    public void registerInvoker(TypeElement mixinType, ExecutableElement method) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(IMessagerEx.MessageType.ACCESSOR_ON_NON_MIXIN_METHOD, (CharSequence)"Found @Invoker annotation on a non-mixin method", (Element)method);
            return;
        }
        AnnotationHandle invoker = AnnotationHandle.of(method, Invoker.class);
        mixinClass.registerInvoker(method, invoker, AnnotatedMixins.shouldRemap(mixinClass, invoker));
    }

    public void registerOverwrite(TypeElement mixinType, ExecutableElement method) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(IMessagerEx.MessageType.OVERWRITE_ON_NON_MIXIN_METHOD, (CharSequence)"Found @Overwrite annotation on a non-mixin method", (Element)method);
            return;
        }
        AnnotationHandle overwrite = AnnotationHandle.of(method, Overwrite.class);
        mixinClass.registerOverwrite(method, overwrite, AnnotatedMixins.shouldRemap(mixinClass, overwrite));
    }

    public void registerShadow(TypeElement mixinType, VariableElement field, AnnotationHandle shadow) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(IMessagerEx.MessageType.SHADOW_ON_NON_MIXIN_ELEMENT, (CharSequence)"Found @Shadow annotation on a non-mixin field", (Element)field);
            return;
        }
        mixinClass.registerShadow(field, shadow, AnnotatedMixins.shouldRemap(mixinClass, shadow));
    }

    public void registerShadow(TypeElement mixinType, ExecutableElement method, AnnotationHandle shadow) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(IMessagerEx.MessageType.SHADOW_ON_NON_MIXIN_ELEMENT, (CharSequence)"Found @Shadow annotation on a non-mixin method", (Element)method);
            return;
        }
        mixinClass.registerShadow(method, shadow, AnnotatedMixins.shouldRemap(mixinClass, shadow));
    }

    public void registerInjector(TypeElement mixinType, ExecutableElement method, AnnotationHandle inject) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(IMessagerEx.MessageType.INJECTOR_ON_NON_MIXIN_METHOD, (CharSequence)("Found " + inject + " annotation on a non-mixin method"), (Element)method);
            return;
        }
        InjectorRemap remap = new InjectorRemap(AnnotatedMixins.shouldRemap(mixinClass, inject));
        mixinClass.registerInjector(method, inject, remap);
        remap.dispatchPendingMessages(this);
    }

    public void registerSoftImplements(TypeElement mixin, AnnotationHandle implementsAnnotation) {
        AnnotatedMixin mixinClass = this.getMixin(mixin);
        if (mixinClass == null) {
            this.printMessage(IMessagerEx.MessageType.SOFT_IMPLEMENTS_ON_NON_MIXIN, (CharSequence)"Found @Implements annotation on a non-mixin class");
            return;
        }
        mixinClass.registerSoftImplements(implementsAnnotation);
    }

    public void onPassStarted() {
        this.mixinsForPass.clear();
    }

    public void onPassCompleted(RoundEnvironment roundEnv) {
        if (!"true".equalsIgnoreCase(this.getOption("disableTargetExport"))) {
            this.targets.write(true);
        }
        for (AnnotatedMixin mixin : roundEnv.processingOver() ? this.mixins.values() : this.mixinsForPass) {
            mixin.runValidators(roundEnv.processingOver() ? IMixinValidator.ValidationPass.FINAL : IMixinValidator.ValidationPass.LATE, this.validators);
        }
    }

    private static boolean shouldRemap(AnnotatedMixin mixinClass, AnnotationHandle annotation) {
        return annotation.getBoolean("remap", mixinClass.remap());
    }

    private static boolean shouldSuppress(Element element, SuppressedBy suppressedBy) {
        if (element == null || suppressedBy == null) {
            return false;
        }
        if (AnnotationHandle.of(element, SuppressWarnings.class).getList().contains(suppressedBy.getToken())) {
            return true;
        }
        return AnnotatedMixins.shouldSuppress(element.getEnclosingElement(), suppressedBy);
    }

    @Override
    public void printMessage(IMessagerEx.MessageType type, CharSequence msg) {
        if (type.isEnabled()) {
            this.printMessage(type.getKind(), type.decorate(msg));
        }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
        this.processingEnv.getMessager().printMessage(kind, msg);
    }

    @Override
    public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element) {
        if (type.isEnabled()) {
            this.printMessage(type.getKind(), type.decorate(msg), element);
        }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element) {
        this.processingEnv.getMessager().printMessage(kind, msg, element);
    }

    @Override
    public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, SuppressedBy suppressedBy) {
        if (type.isEnabled()) {
            this.printMessage(type.getKind(), type.decorate(msg), element, suppressedBy);
        }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, SuppressedBy suppressedBy) {
        if (kind != Diagnostic.Kind.WARNING || !AnnotatedMixins.shouldSuppress(element, suppressedBy)) {
            this.processingEnv.getMessager().printMessage(kind, msg, element);
        }
    }

    @Override
    public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation) {
        if (type.isEnabled()) {
            this.printMessage(type.getKind(), type.decorate(msg), element, annotation);
        }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation) {
        this.processingEnv.getMessager().printMessage(kind, msg, element, annotation);
    }

    @Override
    public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation, SuppressedBy suppressedBy) {
        if (type.isEnabled()) {
            this.printMessage(type.getKind(), type.decorate(msg), element, annotation, suppressedBy);
        }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, SuppressedBy suppressedBy) {
        if (kind != Diagnostic.Kind.WARNING || !AnnotatedMixins.shouldSuppress(element, suppressedBy)) {
            this.processingEnv.getMessager().printMessage(kind, msg, element, annotation);
        }
    }

    @Override
    public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
        if (type.isEnabled()) {
            this.printMessage(type.getKind(), type.decorate(msg), element, annotation, value);
        }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
        this.processingEnv.getMessager().printMessage(kind, msg, element, annotation, value);
    }

    @Override
    public void printMessage(IMessagerEx.MessageType type, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value, SuppressedBy suppressedBy) {
        if (type.isEnabled()) {
            this.printMessage(type.getKind(), type.decorate(msg), element, annotation, value, suppressedBy);
        }
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value, SuppressedBy suppressedBy) {
        if (kind != Diagnostic.Kind.WARNING || !AnnotatedMixins.shouldSuppress(element, suppressedBy)) {
            this.processingEnv.getMessager().printMessage(kind, msg, element, annotation, value);
        }
    }

    @Override
    public TypeHandle getTypeHandle(String name) {
        TypeHandle asmTypeHandle;
        name = name.replace('/', '.');
        Elements elements = this.processingEnv.getElementUtils();
        PackageElement pkg = null;
        int lastDotPos = name.lastIndexOf(46);
        if (lastDotPos > -1) {
            String pkgName = name.substring(0, lastDotPos);
            pkg = elements.getPackageElement(pkgName);
        }
        if (pkg != null && (asmTypeHandle = TypeHandleASM.of(pkg, name.substring(lastDotPos + 1), this)) != null) {
            return asmTypeHandle;
        }
        TypeElement element = this.getTypeElement(name, elements);
        if (element != null) {
            try {
                return new TypeHandle(element, (ITypeHandleProvider)this);
            }
            catch (NullPointerException nullPointerException) {
                // empty catch block
            }
        }
        if (pkg != null) {
            return new TypeHandle(pkg, name, this);
        }
        return null;
    }

    @Override
    public TypeHandle getTypeHandle(Object type) {
        if (type instanceof TypeHandle) {
            return (TypeHandle)type;
        }
        if (type instanceof DeclaredType) {
            return this.getTypeHandle(TypeUtils.getInternalName((DeclaredType)type));
        }
        if (type instanceof Type) {
            return this.getTypeHandle(((Type)type).getClassName());
        }
        if (type instanceof TypeElement) {
            return this.getTypeHandle(TypeUtils.getInternalName((TypeElement)type));
        }
        if (type instanceof String) {
            return this.getTypeHandle(type.toString());
        }
        return null;
    }

    private TypeElement getTypeElement(String name, Elements elements) {
        TypeElement element = elements.getTypeElement(name);
        if (element != null || name.indexOf(36) < 0) {
            return element;
        }
        int lastDotPos = name.lastIndexOf(46);
        String pkg = lastDotPos > -1 ? name.substring(0, lastDotPos) : "";
        name = name.substring(pkg.length());
        element = elements.getTypeElement(pkg + name.replace('$', '.'));
        if (element != null) {
            return element;
        }
        char[] source = name.toCharArray();
        char[] dest = new char[source.length];
        int occurs = 0;
        for (int offset = 0; offset < source.length; ++offset) {
            if (source[offset] != '$') continue;
            ++occurs;
        }
        if (occurs > 10 || occurs < 2) {
            return null;
        }
        for (int mask = 1; mask < 1 << occurs && element == null; ++mask) {
            int index = 0;
            for (int offset = source.length - 1; offset >= 0; --offset) {
                dest[offset] = source[offset] == '$' && (mask & 1 << index++) != 0 ? 46 : source[offset];
            }
            element = elements.getTypeElement(pkg + new String(dest));
        }
        return element;
    }

    @Override
    public TypeHandle getSimulatedHandle(String name, TypeMirror simulatedTarget) {
        int lastDotPos = (name = name.replace('/', '.')).lastIndexOf(46);
        if (lastDotPos > -1) {
            String pkg = name.substring(0, lastDotPos);
            PackageElement packageElement = this.processingEnv.getElementUtils().getPackageElement(pkg);
            if (packageElement != null) {
                return new TypeHandleSimulated(packageElement, name, simulatedTarget, this);
            }
        }
        return new TypeHandleSimulated(name, simulatedTarget, (ITypeHandleProvider)this);
    }

    @Override
    public String getJavadoc(Element element) {
        Elements elements = this.processingEnv.getElementUtils();
        return elements.getDocComment(element);
    }

    public static AnnotatedMixins getMixinsForEnvironment(ProcessingEnvironment processingEnv) {
        AnnotatedMixins mixins = instances.get(processingEnv);
        if (mixins == null) {
            mixins = new AnnotatedMixins(processingEnv);
            instances.put(processingEnv, mixins);
        }
        return mixins;
    }
}

