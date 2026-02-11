package org.spongepowered.asm.mixin.transformer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.refmap.IClassReferenceMapper;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.mixin.refmap.RemappingReferenceMapper;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.mixin.transformer.PluginHandle;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Lazy;
import org.spongepowered.asm.util.VersionNumber;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.annotations.SerializedName;

final class MixinConfig
implements Comparable<MixinConfig>,
IMixinConfig {
    private static int configOrder = 0;
    private static final Set<String> globalMixinList = new HashSet<String>();
    private final ILogger logger = MixinService.getService().getLogger("mixin");
    private final transient Map<String, List<MixinInfo>> mixinMapping = new HashMap<String, List<MixinInfo>>();
    private final transient Set<String> unhandledTargets = new HashSet<String>();
    private final transient List<MixinInfo> pendingMixins = new ArrayList<MixinInfo>();
    private final transient List<MixinInfo> mixins = new ArrayList<MixinInfo>();
    private transient Config handle;
    private transient MixinConfig parent;
    @SerializedName(value="parent")
    private String parentName;
    @SerializedName(value="target")
    private String selector;
    @SerializedName(value="minVersion")
    private String version;
    @SerializedName(value="requiredFeatures")
    private List<String> requiredFeatures;
    @SerializedName(value="compatibilityLevel")
    private String compatibility;
    @SerializedName(value="required")
    private Boolean requiredValue;
    private transient boolean required;
    @SerializedName(value="priority")
    private int priority = -1;
    @SerializedName(value="mixinPriority")
    private int mixinPriority = -1;
    @SerializedName(value="package")
    private String mixinPackage;
    @SerializedName(value="mixins")
    private List<String> mixinClasses;
    @SerializedName(value="client")
    private List<String> mixinClassesClient;
    @SerializedName(value="server")
    private List<String> mixinClassesServer;
    @SerializedName(value="setSourceFile")
    private boolean setSourceFile = false;
    @SerializedName(value="refmap")
    private String refMapperConfig;
    @SerializedName(value="refmapWrapper")
    private String refMapperWrapper;
    @SerializedName(value="verbose")
    private boolean verboseLogging;
    private final transient int order = configOrder++;
    private final transient List<IListener> listeners = new ArrayList<IListener>();
    private transient IMixinService service;
    private transient MixinEnvironment env;
    private transient String name;
    private transient IMixinConfigSource source;
    @SerializedName(value="plugin")
    private String pluginClassName;
    @SerializedName(value="injectors")
    private InjectorOptions injectorOptions;
    @SerializedName(value="overwrites")
    private OverwriteOptions overwriteOptions;
    private transient PluginHandle plugin;
    private transient IReferenceMapper refMapper;
    private transient boolean initialised = false;
    private transient boolean prepared = false;
    private transient boolean visited = false;
    private transient MixinEnvironment.CompatibilityLevel compatibilityLevel = MixinEnvironment.CompatibilityLevel.DEFAULT;
    private transient int warnedClassVersion = 0;
    private transient Map<String, Lazy> decorations;

    private MixinConfig() {
    }

    private boolean onLoad(IMixinService service, String name, MixinEnvironment fallbackEnvironment, IMixinConfigSource source) {
        this.service = service;
        this.name = name;
        this.source = source;
        if (!Strings.isNullOrEmpty(this.parentName)) {
            return true;
        }
        this.env = this.parseSelector(this.selector, fallbackEnvironment);
        this.verboseLogging |= this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
        this.required = this.requiredValue != null && this.requiredValue != false && !this.env.getOption(MixinEnvironment.Option.IGNORE_REQUIRED);
        this.initPriority(1000, 1000);
        if (this.injectorOptions == null) {
            this.injectorOptions = new InjectorOptions();
        }
        if (this.overwriteOptions == null) {
            this.overwriteOptions = new OverwriteOptions();
        }
        return this.postInit();
    }

    String getParentName() {
        return this.parentName;
    }

    boolean assignParent(Config parentConfig) {
        if (this.parent != null) {
            throw new MixinInitialisationError("Mixin config " + this.name + " was already initialised");
        }
        if (parentConfig.get() == this) {
            throw new MixinInitialisationError("Mixin config " + this.name + " cannot be its own parent");
        }
        this.parent = parentConfig.get();
        if (!this.parent.initialised) {
            throw new MixinInitialisationError("Mixin config " + this.name + " attempted to assign uninitialised parent config. This probably means that there is an indirect loop in the mixin configs: child -> parent -> child");
        }
        this.env = this.parseSelector(this.selector, this.parent.env);
        this.verboseLogging |= this.env.getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
        this.required = this.requiredValue == null ? this.parent.required : this.requiredValue != false && !this.env.getOption(MixinEnvironment.Option.IGNORE_REQUIRED);
        this.initPriority(this.parent.priority, this.parent.mixinPriority);
        if (this.injectorOptions == null) {
            this.injectorOptions = this.parent.injectorOptions;
        } else {
            this.injectorOptions.mergeFrom(this.parent.injectorOptions);
        }
        if (this.overwriteOptions == null) {
            this.overwriteOptions = this.parent.overwriteOptions;
        } else {
            this.overwriteOptions.mergeFrom(this.parent.overwriteOptions);
        }
        this.setSourceFile |= this.parent.setSourceFile;
        this.verboseLogging |= this.parent.verboseLogging;
        return this.postInit();
    }

    private void initPriority(int defaultPriority, int defaultMixinPriority) {
        if (this.priority < 0) {
            this.priority = defaultPriority;
        }
        if (this.mixinPriority < 0) {
            this.mixinPriority = defaultMixinPriority;
        }
    }

    private boolean postInit() throws MixinInitialisationError {
        if (this.initialised) {
            throw new MixinInitialisationError("Mixin config " + this.name + " was already initialised.");
        }
        this.initialised = true;
        this.initCompatibilityLevel();
        this.initExtensions();
        return this.checkVersion() && this.checkFeatures();
    }

    private void initCompatibilityLevel() {
        this.compatibilityLevel = MixinEnvironment.getCompatibilityLevel();
        if (this.compatibility == null) {
            return;
        }
        String strCompatibility = this.compatibility.trim().toUpperCase(Locale.ROOT);
        try {
            this.compatibilityLevel = MixinEnvironment.CompatibilityLevel.valueOf(strCompatibility);
        }
        catch (IllegalArgumentException ex) {
            throw new MixinInitialisationError(String.format("Mixin config %s specifies compatibility level %s which is not recognised", this.name, strCompatibility));
        }
        MixinEnvironment.CompatibilityLevel currentLevel = MixinEnvironment.getCompatibilityLevel();
        if (this.compatibilityLevel == currentLevel) {
            return;
        }
        if (currentLevel.isAtLeast(this.compatibilityLevel) && !currentLevel.canSupport(this.compatibilityLevel)) {
            throw new MixinInitialisationError(String.format("Mixin config %s requires compatibility level %s which is too old", new Object[]{this.name, this.compatibilityLevel}));
        }
        if (!currentLevel.canElevateTo(this.compatibilityLevel)) {
            throw new MixinInitialisationError(String.format("Mixin config %s requires compatibility level %s which is prohibited by %s", new Object[]{this.name, this.compatibilityLevel, currentLevel}));
        }
        MixinEnvironment.CompatibilityLevel minCompatibilityLevel = MixinEnvironment.getMinCompatibilityLevel();
        if (this.compatibilityLevel.isLessThan(minCompatibilityLevel)) {
            this.logger.log(this.verboseLogging ? Level.INFO : Level.DEBUG, "Compatibility level {} specified by {} is lower than the default level supported by the current mixin service ({}).", new Object[]{this.compatibilityLevel, this, minCompatibilityLevel});
        }
        if (MixinEnvironment.CompatibilityLevel.MAX_SUPPORTED.isLessThan(this.compatibilityLevel)) {
            this.logger.log(this.verboseLogging ? Level.WARN : Level.DEBUG, "Compatibility level {} specified by {} is higher than the maximum level supported by this version of mixin ({}).", new Object[]{this.compatibilityLevel, this, MixinEnvironment.CompatibilityLevel.MAX_SUPPORTED});
        }
        MixinEnvironment.setCompatibilityLevel(this.compatibilityLevel);
    }

    void checkCompatibilityLevel(MixinInfo mixin, int majorVersion, int minorVersion) {
        if (majorVersion <= this.compatibilityLevel.getClassMajorVersion()) {
            return;
        }
        Level logLevel = this.verboseLogging && majorVersion > this.warnedClassVersion ? Level.WARN : Level.DEBUG;
        String message = majorVersion > MixinEnvironment.CompatibilityLevel.MAX_SUPPORTED.getClassMajorVersion() ? "the current version of Mixin" : "the declared compatibility level";
        this.warnedClassVersion = majorVersion;
        this.logger.log(logLevel, "{}: Class version {} required is higher than the class version supported by {} ({} supports class version {})", new Object[]{mixin, majorVersion, message, this.compatibilityLevel, this.compatibilityLevel.getClassMajorVersion()});
    }

    private MixinEnvironment parseSelector(String target, MixinEnvironment fallbackEnvironment) {
        if (target != null) {
            String[] selectors;
            for (String sel : selectors = target.split("[&\\| ]")) {
                sel = sel.trim();
                Pattern environmentSelector = Pattern.compile("^@env(?:ironment)?\\(([A-Z]+)\\)$");
                Matcher environmentSelectorMatcher = environmentSelector.matcher(sel);
                if (!environmentSelectorMatcher.matches()) continue;
                return MixinEnvironment.getEnvironment(MixinEnvironment.Phase.forName(environmentSelectorMatcher.group(1)));
            }
            MixinEnvironment.Phase phase = MixinEnvironment.Phase.forName(target);
            if (phase != null) {
                return MixinEnvironment.getEnvironment(phase);
            }
        }
        return fallbackEnvironment;
    }

    private void initExtensions() {
        if (this.injectorOptions.injectionPoints != null) {
            for (String injectionPointClassName : this.injectorOptions.injectionPoints) {
                this.initInjectionPoint(injectionPointClassName, this.injectorOptions.namespace);
            }
        }
        if (this.injectorOptions.dynamicSelectors != null) {
            for (String dynamicSelectorClassName : this.injectorOptions.dynamicSelectors) {
                this.initDynamicSelector(dynamicSelectorClassName, this.injectorOptions.namespace);
            }
        }
    }

    private void initInjectionPoint(String className, String namespace) {
        block4: {
            try {
                Class<?> injectionPointClass = this.findExtensionClass(className, InjectionPoint.class, "injection point");
                if (injectionPointClass == null) break block4;
                try {
                    injectionPointClass.getMethod("find", String.class, InsnList.class, Collection.class);
                }
                catch (NoSuchMethodException cnfe) {
                    this.logger.error("Unable to register injection point {} for {}, the class is not compatible with this version of Mixin", className, this, cnfe);
                    return;
                }
                InjectionPoint.register(injectionPointClass, namespace);
            }
            catch (Throwable th) {
                this.logger.catching(th);
            }
        }
    }

    private void initDynamicSelector(String className, String namespace) {
        try {
            Class<?> dynamicSelectorClass = this.findExtensionClass(className, ITargetSelectorDynamic.class, "dynamic selector");
            if (dynamicSelectorClass != null) {
                TargetSelector.register(dynamicSelectorClass, namespace);
            }
        }
        catch (Throwable th) {
            this.logger.catching(th);
        }
    }

    private Class<?> findExtensionClass(String className, Class<?> superType, String extensionType) {
        Class<?> extensionClass = null;
        try {
            extensionClass = this.service.getClassProvider().findClass(className, true);
        }
        catch (ClassNotFoundException cnfe) {
            this.logger.error("Unable to register {} {} for {}, the specified class was not found", extensionType, className, this, cnfe);
            return null;
        }
        if (!superType.isAssignableFrom(extensionClass)) {
            this.logger.error("Unable to register {} {} for {}, class is not assignable to {}", extensionType, className, this, superType);
            return null;
        }
        return extensionClass;
    }

    private boolean checkVersion() throws MixinInitialisationError {
        VersionNumber curVersion;
        VersionNumber minVersion;
        if (this.version == null) {
            if (this.parent != null && this.parent.version != null) {
                return true;
            }
            if (this.requiredFeatures == null || this.requiredFeatures.isEmpty()) {
                this.logger.debug("Mixin config {} does not specify \"minVersion\" or \"requiredFeatures\" property", this.name);
            }
        }
        if ((minVersion = VersionNumber.parse(this.version)).compareTo(curVersion = VersionNumber.parse(this.env.getVersion())) > 0) {
            this.logger.warn("Mixin config {} requires mixin subsystem version {} but {} was found. The mixin config will not be applied.", this.name, minVersion, curVersion);
            if (this.required) {
                throw new MixinInitialisationError("Required mixin config " + this.name + " requires mixin subsystem version " + minVersion);
            }
            return false;
        }
        return true;
    }

    private boolean checkFeatures() throws MixinInitialisationError {
        if (this.requiredFeatures == null || this.requiredFeatures.isEmpty()) {
            return true;
        }
        LinkedHashSet<String> missingFeatures = new LinkedHashSet<String>();
        for (String featureId : this.requiredFeatures) {
            if (MixinEnvironment.Feature.isActive(featureId = featureId.trim().toUpperCase(Locale.ROOT))) continue;
            missingFeatures.add(featureId);
        }
        if (missingFeatures.isEmpty()) {
            return true;
        }
        String strMissingFeatures = Joiner.on(", ").join(missingFeatures);
        this.logger.warn("Mixin config {} requires features [{}] which are not available. The mixin config will not be applied.", this.name, strMissingFeatures);
        if (this.required) {
            throw new MixinInitialisationError("Required mixin config " + this.name + " requires features [" + strMissingFeatures + " which are not available");
        }
        return false;
    }

    void addListener(IListener listener) {
        this.listeners.add(listener);
    }

    void onSelect() {
        this.plugin = new PluginHandle(this, this.service, this.pluginClassName);
        this.plugin.onLoad(Strings.nullToEmpty(this.mixinPackage));
        if (Strings.isNullOrEmpty(this.mixinPackage)) {
            return;
        }
        if (!this.mixinPackage.endsWith(".")) {
            this.mixinPackage = this.mixinPackage + ".";
        }
        boolean suppressRefMapWarning = false;
        if (this.refMapperConfig == null) {
            this.refMapperConfig = this.plugin.getRefMapperConfig();
            if (this.refMapperConfig == null) {
                suppressRefMapWarning = true;
                this.refMapperConfig = "mixin.refmap.json";
            }
        }
        this.refMapper = ReferenceMapper.read(this.refMapperConfig);
        if (!suppressRefMapWarning && this.refMapper.isDefault() && !this.env.getOption(MixinEnvironment.Option.DISABLE_REFMAP)) {
            this.logger.warn("Reference map '{}' for {} could not be read. If this is a development environment you can ignore this message", this.refMapperConfig, this);
        }
        if (this.env.getOption(MixinEnvironment.Option.REFMAP_REMAP)) {
            this.refMapper = RemappingReferenceMapper.of(this.env, this.refMapper);
        }
        if (this.refMapperWrapper != null) {
            String wrapperName = this.mixinPackage + this.refMapperWrapper;
            try {
                Class<?> wrapperCls = this.service.getClassProvider().findClass(wrapperName, true);
                Constructor<?> ctr = wrapperCls.getConstructor(MixinEnvironment.class, IReferenceMapper.class);
                this.refMapper = (IReferenceMapper)ctr.newInstance(this.env, this.refMapper);
            }
            catch (ClassNotFoundException e) {
                this.logger.error("Reference map wrapper '{}' could not be found: ", wrapperName, e);
            }
            catch (ReflectiveOperationException e) {
                this.logger.error("Reference map wrapper '{}' could not be created: ", wrapperName, e);
            }
            catch (SecurityException e) {
                this.logger.error("Reference map wrapper '{}' could not be created: ", wrapperName, e);
            }
        }
    }

    void prepare(Extensions extensions) {
        if (this.prepared) {
            return;
        }
        this.prepared = true;
        this.prepareMixins("mixins", this.mixinClasses, false, extensions);
        switch (this.env.getSide()) {
            case CLIENT: {
                this.prepareMixins("client", this.mixinClassesClient, false, extensions);
                break;
            }
            case SERVER: {
                this.prepareMixins("server", this.mixinClassesServer, false, extensions);
                break;
            }
            default: {
                this.logger.warn("Mixin environment was unable to detect the current side, sided mixins will not be applied", new Object[0]);
            }
        }
    }

    void postInitialise(Extensions extensions) {
        if (this.plugin != null) {
            List<String> pluginMixins = this.plugin.getMixins();
            this.prepareMixins("companion plugin", pluginMixins, true, extensions);
        }
        Iterator<MixinInfo> iter = this.mixins.iterator();
        while (iter.hasNext()) {
            MixinInfo mixin = iter.next();
            try {
                mixin.validate();
                for (IListener listener : this.listeners) {
                    listener.onInit(mixin);
                }
            }
            catch (InvalidMixinException ex) {
                this.logger.error(ex.getMixin() + ": " + ex.getMessage(), ex);
                this.removeMixin(mixin);
                iter.remove();
            }
            catch (Exception ex) {
                this.logger.error(ex.getMessage(), ex);
                this.removeMixin(mixin);
                iter.remove();
            }
        }
    }

    private void removeMixin(MixinInfo remove) {
        for (List<MixinInfo> mixinsFor : this.mixinMapping.values()) {
            Iterator<MixinInfo> iter = mixinsFor.iterator();
            while (iter.hasNext()) {
                if (remove != iter.next()) continue;
                iter.remove();
            }
        }
    }

    private void prepareMixins(String collectionName, List<String> mixinClasses, boolean ignorePlugin, Extensions extensions) {
        if (mixinClasses == null) {
            return;
        }
        if (Strings.isNullOrEmpty(this.mixinPackage)) {
            if (mixinClasses.size() > 0) {
                this.logger.error("{} declares mixin classes in {} but does not specify a package, {} orphaned mixins will not be loaded: {}", this, collectionName, mixinClasses.size(), mixinClasses);
            }
            return;
        }
        for (String mixinClass : mixinClasses) {
            Iterator<IListener> fqMixinClass = this.mixinPackage + mixinClass;
            if (mixinClass == null || globalMixinList.contains(fqMixinClass)) continue;
            MixinInfo mixin = null;
            try {
                mixin = new MixinInfo(this.service, this, mixinClass, this.plugin, ignorePlugin, extensions);
                this.pendingMixins.add(mixin);
                globalMixinList.add((String)((Object)fqMixinClass));
            }
            catch (InvalidMixinException ex) {
                if (this.required) {
                    throw ex;
                }
                this.logger.error(ex.getMessage(), ex);
            }
            catch (Exception ex) {
                if (this.required) {
                    throw new InvalidMixinException((IMixinInfo)mixin, "Error initialising mixin " + mixin + " - " + ex.getClass() + ": " + ex.getMessage(), (Throwable)ex);
                }
                this.logger.error(ex.getMessage(), ex);
            }
        }
        for (MixinInfo mixin : this.pendingMixins) {
            try {
                mixin.parseTargets();
                if (mixin.getTargetClasses().size() <= 0) continue;
                for (String targetClass : mixin.getTargetClasses()) {
                    String targetClassName = targetClass.replace('/', '.');
                    this.mixinsFor(targetClassName).add(mixin);
                    this.unhandledTargets.add(targetClassName);
                }
                for (IListener listener : this.listeners) {
                    listener.onPrepare(mixin);
                }
                this.mixins.add(mixin);
            }
            catch (InvalidMixinException ex) {
                if (this.required) {
                    throw ex;
                }
                this.logger.error(ex.getMessage(), ex);
            }
            catch (Exception ex) {
                if (this.required) {
                    throw new InvalidMixinException((IMixinInfo)mixin, "Error initialising mixin " + mixin + " - " + ex.getClass() + ": " + ex.getMessage(), (Throwable)ex);
                }
                this.logger.error(ex.getMessage(), ex);
            }
        }
        this.pendingMixins.clear();
    }

    void postApply(String transformedName, ClassNode targetClass) {
        this.unhandledTargets.remove(transformedName);
    }

    public Config getHandle() {
        if (this.handle == null) {
            this.handle = new Config(this);
        }
        return this.handle;
    }

    @Override
    public boolean isRequired() {
        return this.required;
    }

    @Override
    public MixinEnvironment getEnvironment() {
        return this.env;
    }

    MixinConfig getParent() {
        return this.parent;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public IMixinConfigSource getSource() {
        return this.source;
    }

    @Override
    public String getCleanSourceId() {
        if (this.source == null) {
            return null;
        }
        String sourceId = this.source.getId();
        if (sourceId == null) {
            return null;
        }
        return sourceId.replaceAll("[^A-Za-z]", "");
    }

    @Override
    public String getMixinPackage() {
        return Strings.nullToEmpty(this.mixinPackage);
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    public int getDefaultMixinPriority() {
        return this.mixinPriority;
    }

    public int getDefaultRequiredInjections() {
        return this.injectorOptions.defaultRequireValue;
    }

    public String getDefaultInjectorGroup() {
        String defaultGroup = this.injectorOptions.defaultGroup;
        return defaultGroup != null && !defaultGroup.isEmpty() ? defaultGroup : "default";
    }

    public boolean conformOverwriteVisibility() {
        return this.overwriteOptions.conformAccessModifiers;
    }

    public boolean requireOverwriteAnnotations() {
        return this.overwriteOptions.requireOverwriteAnnotations;
    }

    public int getMaxShiftByValue() {
        return Math.min(Math.max(this.injectorOptions.maxShiftBy, 0), 5);
    }

    public boolean select(MixinEnvironment environment) {
        this.visited = true;
        return this.env == environment;
    }

    boolean isVisited() {
        return this.visited;
    }

    int getDeclaredMixinCount() {
        return MixinConfig.getCollectionSize(this.mixinClasses, this.mixinClassesClient, this.mixinClassesServer);
    }

    int getMixinCount() {
        return this.mixins.size();
    }

    public List<String> getClasses() {
        if (Strings.isNullOrEmpty(this.mixinPackage)) {
            return Collections.emptyList();
        }
        ImmutableList.Builder list = ImmutableList.builder();
        for (List classes : new List[]{this.mixinClasses, this.mixinClassesClient, this.mixinClassesServer}) {
            if (classes == null) continue;
            for (String className : classes) {
                list.add(this.mixinPackage + className);
            }
        }
        return list.build();
    }

    public boolean shouldSetSourceFile() {
        return this.setSourceFile;
    }

    public IReferenceMapper getReferenceMapper() {
        if (this.env.getOption(MixinEnvironment.Option.DISABLE_REFMAP)) {
            return ReferenceMapper.DEFAULT_MAPPER;
        }
        this.refMapper.setContext(this.env.getRefmapObfuscationContext());
        return this.refMapper;
    }

    String remapClassName(String className, String reference) {
        IReferenceMapper mapper = this.getReferenceMapper();
        if (mapper instanceof IClassReferenceMapper) {
            return ((IClassReferenceMapper)((Object)mapper)).remapClassName(className, reference);
        }
        return mapper.remap(className, reference);
    }

    @Override
    public IMixinConfigPlugin getPlugin() {
        return this.plugin.get();
    }

    public Set<String> getTargetsSet() {
        return this.mixinMapping.keySet();
    }

    @Override
    public Set<String> getTargets() {
        return Collections.unmodifiableSet(this.mixinMapping.keySet());
    }

    public Set<String> getUnhandledTargets() {
        return Collections.unmodifiableSet(this.unhandledTargets);
    }

    @Override
    public <V> void decorate(String key, V value) {
        if (this.decorations == null) {
            this.decorations = new HashMap<String, Lazy>();
        }
        if (this.decorations.containsKey(key)) {
            throw new IllegalArgumentException(String.format("Decoration with key '%s' already exists on config %s", key, this));
        }
        this.decorations.put(key, Lazy.of(value));
    }

    @Override
    public boolean hasDecoration(String key) {
        return this.decorations != null && this.decorations.get(key) != null;
    }

    @Override
    public <V> V getDecoration(String key) {
        if (this.decorations == null) {
            return null;
        }
        Lazy value = this.decorations.get(key);
        return value == null ? null : (V)value.get();
    }

    public Level getLoggingLevel() {
        return this.verboseLogging ? Level.INFO : Level.DEBUG;
    }

    public boolean isVerboseLogging() {
        return this.verboseLogging;
    }

    public boolean packageMatch(String className) {
        return !Strings.isNullOrEmpty(this.mixinPackage) && className.startsWith(this.mixinPackage);
    }

    public boolean hasMixinsFor(String targetClass) {
        return this.mixinMapping.containsKey(targetClass);
    }

    boolean hasPendingMixinsFor(String targetClass) {
        if (this.packageMatch(targetClass)) {
            return false;
        }
        for (MixinInfo pendingMixin : this.pendingMixins) {
            if (!pendingMixin.hasDeclaredTarget(targetClass)) continue;
            return true;
        }
        return false;
    }

    public List<MixinInfo> getMixinsFor(String targetClass) {
        return this.mixinsFor(targetClass);
    }

    private List<MixinInfo> mixinsFor(String targetClass) {
        List<MixinInfo> mixins = this.mixinMapping.get(targetClass);
        if (mixins == null) {
            mixins = new ArrayList<MixinInfo>();
            this.mixinMapping.put(targetClass, mixins);
        }
        return mixins;
    }

    public List<String> reloadMixin(String mixinClass, ClassNode classNode) {
        for (MixinInfo mixin : this.mixins) {
            if (!mixin.getClassName().equals(mixinClass)) continue;
            mixin.reloadMixin(classNode);
            return mixin.getTargetClasses();
        }
        return Collections.emptyList();
    }

    public String toString() {
        return this.name;
    }

    @Override
    public int compareTo(MixinConfig other) {
        if (other == null) {
            return 0;
        }
        if (other.priority == this.priority) {
            return Integer.compare(this.order, other.order);
        }
        return this.priority < other.priority ? -1 : 1;
    }

    static Config create(String configFile, MixinEnvironment outer, IMixinConfigSource source) {
        Set disabledMixinConfigs = GlobalProperties.get(GlobalProperties.Keys.CLEANROOM_DISABLE_MIXIN_CONFIGS, Collections.emptySet());
        if (disabledMixinConfigs.contains(configFile)) {
            return null;
        }
        try {
            IMixinService service = MixinService.getService();
            InputStream resource = service.getResourceAsStream(configFile);
            if (resource == null) {
                throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile));
            }
            MixinConfig config = new Gson().fromJson(new InputStreamReader(resource), MixinConfig.class);
            if (config.onLoad(service, configFile, outer, source)) {
                return config.getHandle();
            }
            return null;
        }
        catch (IllegalArgumentException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IllegalArgumentException(String.format("The specified resource '%s' was invalid or could not be read", configFile), ex);
        }
    }

    private static int getCollectionSize(Collection<?> ... collections) {
        int total = 0;
        for (Collection<?> collection : collections) {
            if (collection == null) continue;
            total += collection.size();
        }
        return total;
    }

    static interface IListener {
        public void onPrepare(MixinInfo var1);

        public void onInit(MixinInfo var1);
    }

    static class OverwriteOptions {
        @SerializedName(value="conformVisibility")
        boolean conformAccessModifiers = true;
        @SerializedName(value="requireAnnotations")
        boolean requireOverwriteAnnotations;

        OverwriteOptions() {
        }

        void mergeFrom(OverwriteOptions parent) {
            this.conformAccessModifiers |= parent.conformAccessModifiers;
            this.requireOverwriteAnnotations |= parent.requireOverwriteAnnotations;
        }
    }

    static class InjectorOptions {
        @SerializedName(value="defaultRequire")
        int defaultRequireValue = 0;
        @SerializedName(value="defaultGroup")
        String defaultGroup = "default";
        @SerializedName(value="namespace")
        String namespace;
        @SerializedName(value="injectionPoints")
        List<String> injectionPoints;
        @SerializedName(value="dynamicSelectors")
        List<String> dynamicSelectors;
        @SerializedName(value="maxShiftBy")
        int maxShiftBy = 0;

        InjectorOptions() {
        }

        void mergeFrom(InjectorOptions parent) {
            if (this.defaultRequireValue == 0) {
                this.defaultRequireValue = parent.defaultRequireValue;
            }
            if ("default".equals(this.defaultGroup)) {
                this.defaultGroup = parent.defaultGroup;
            }
            if (this.maxShiftBy == 0) {
                this.maxShiftBy = parent.maxShiftBy;
            }
        }
    }
}

