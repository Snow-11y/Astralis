package org.spongepowered.asm.launch.platform;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.obfuscation.FMLLegacyDevRemapper;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;
import org.spongepowered.asm.util.IConsumer;

public class MixinPlatformAgentFMLLegacy
extends MixinPlatformAgentAbstract
implements IMixinPlatformServiceAgent {
    private static final Set<String> loadedCoreMods = new HashSet<String>();
    private File file;
    private String fileName;
    private ITweaker coreModWrapper;
    private Class<?> clCoreModManager;
    private boolean initInjectionState;
    static MixinAppender appender;
    static org.apache.logging.log4j.core.Logger log;
    static Level oldLevel;

    @Override
    public IMixinPlatformAgent.AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        if (this.getCoreModManagerClass() == null) {
            return IMixinPlatformAgent.AcceptResult.INVALID;
        }
        if (!(handle instanceof ContainerHandleURI) || super.accept(manager, handle) != IMixinPlatformAgent.AcceptResult.ACCEPTED) {
            return IMixinPlatformAgent.AcceptResult.REJECTED;
        }
        this.file = ((ContainerHandleURI)handle).getFile();
        this.fileName = this.file.getName();
        this.coreModWrapper = this.initFMLCoreMod();
        return this.coreModWrapper != null ? IMixinPlatformAgent.AcceptResult.ACCEPTED : IMixinPlatformAgent.AcceptResult.REJECTED;
    }

    private ITweaker initFMLCoreMod() {
        try {
            if ("true".equalsIgnoreCase(this.handle.getAttribute("ForceLoadAsMod"))) {
                MixinPlatformAgentAbstract.logger.debug("ForceLoadAsMod was specified for {}, attempting force-load", this.fileName);
                this.loadAsMod();
            }
            return this.injectCorePlugin();
        }
        catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.catching(ex);
            return null;
        }
    }

    private void loadAsMod() {
        try {
            MixinPlatformAgentFMLLegacy.getIgnoredMods(this.clCoreModManager).remove(this.fileName);
        }
        catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.catching(ex);
        }
        if (this.handle.getAttribute("FMLCorePluginContainsFMLMod") != null) {
            if (this.isIgnoredReparseable()) {
                MixinPlatformAgentAbstract.logger.debug("Ignoring request to add {} to reparseable coremod collection - it is on the class path", this.fileName);
                return;
            }
            this.addReparseableJar();
        }
    }

    private boolean isIgnoredReparseable() {
        return System.getProperty("java.class.path").contains(this.fileName);
    }

    private void addReparseableJar() {
        try {
            Method mdGetReparsedCoremods = this.clCoreModManager.getDeclaredMethod(GlobalProperties.getString(GlobalProperties.Keys.FML_GET_REPARSEABLE_COREMODS, "getReparseableCoremods"), new Class[0]);
            List reparsedCoremods = (List)mdGetReparsedCoremods.invoke(null, new Object[0]);
            if (!reparsedCoremods.contains(this.fileName)) {
                MixinPlatformAgentAbstract.logger.debug("Adding {} to reparseable coremod collection", this.fileName);
                reparsedCoremods.add(this.fileName);
            }
        }
        catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.catching(ex);
        }
    }

    private ITweaker injectCorePlugin() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String coreModName = this.handle.getAttribute("FMLCorePlugin");
        if (coreModName == null) {
            return null;
        }
        if (this.isAlreadyInjected(coreModName)) {
            MixinPlatformAgentAbstract.logger.debug("{} has core plugin {}. Skipping because it was already injected.", this.fileName, coreModName);
            return null;
        }
        MixinPlatformAgentAbstract.logger.debug("{} has core plugin {}. Injecting it into FML for co-initialisation:", this.fileName, coreModName);
        Method mdLoadCoreMod = this.clCoreModManager.getDeclaredMethod(GlobalProperties.getString(GlobalProperties.Keys.FML_LOAD_CORE_MOD, "loadCoreMod"), LaunchClassLoader.class, String.class, File.class);
        mdLoadCoreMod.setAccessible(true);
        ITweaker wrapper = (ITweaker)mdLoadCoreMod.invoke(null, Launch.classLoader, coreModName, this.file);
        if (wrapper == null) {
            MixinPlatformAgentAbstract.logger.debug("Core plugin {} could not be loaded.", coreModName);
            return null;
        }
        this.initInjectionState = MixinPlatformAgentFMLLegacy.isTweakerQueued("FMLInjectionAndSortingTweaker");
        loadedCoreMods.add(coreModName);
        return wrapper;
    }

    private boolean isAlreadyInjected(String coreModName) {
        if (loadedCoreMods.contains(coreModName)) {
            return true;
        }
        try {
            List tweakers = (List)GlobalProperties.get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKS);
            if (tweakers == null) {
                return false;
            }
            for (ITweaker tweaker : tweakers) {
                Class<?> tweakClass = tweaker.getClass();
                if (!"FMLPluginWrapper".equals(tweakClass.getSimpleName())) continue;
                Field fdCoreModInstance = tweakClass.getField("coreModInstance");
                fdCoreModInstance.setAccessible(true);
                Object coreMod = fdCoreModInstance.get(tweaker);
                if (!coreModName.equals(coreMod.getClass().getName())) continue;
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    @Override
    public String getPhaseProvider() {
        return MixinPlatformAgentFMLLegacy.class.getName() + "$PhaseProvider";
    }

    @Override
    public void prepare() {
        this.initInjectionState |= MixinPlatformAgentFMLLegacy.isTweakerQueued("FMLInjectionAndSortingTweaker");
    }

    @Override
    public void inject() {
        if (this.coreModWrapper != null && this.checkForCoInitialisation()) {
            MixinPlatformAgentAbstract.logger.debug("FML agent is co-initiralising coremod instance {} for {}", this.coreModWrapper, this.handle);
            this.coreModWrapper.injectIntoClassLoader(Launch.classLoader);
        }
    }

    protected final boolean checkForCoInitialisation() {
        boolean injectionTweaker = MixinPlatformAgentFMLLegacy.isTweakerQueued("FMLInjectionAndSortingTweaker");
        boolean terminalTweaker = MixinPlatformAgentFMLLegacy.isTweakerQueued("TerminalTweaker");
        if (this.initInjectionState && terminalTweaker || injectionTweaker) {
            MixinPlatformAgentAbstract.logger.debug("FML agent is skipping co-init for {} because FML will inject it normally", this.coreModWrapper);
            return false;
        }
        return !MixinPlatformAgentFMLLegacy.isTweakerQueued("FMLDeobfTweaker");
    }

    private Class<?> getCoreModManagerClass() {
        if (this.clCoreModManager != null) {
            return this.clCoreModManager;
        }
        try {
            try {
                this.clCoreModManager = Class.forName(GlobalProperties.getString(GlobalProperties.Keys.FML_CORE_MOD_MANAGER, "net.minecraftforge.fml.relauncher.CoreModManager"));
            }
            catch (ClassNotFoundException ex) {
                this.clCoreModManager = Class.forName("cpw.mods.fml.relauncher.CoreModManager");
            }
        }
        catch (ClassNotFoundException ex) {
            MixinPlatformAgentAbstract.logger.info("FML platform manager could not load class {}. Proceeding without FML support.", ex.getMessage());
        }
        return this.clCoreModManager;
    }

    private static boolean isTweakerQueued(String tweakerName) {
        for (String tweaker : (List)GlobalProperties.get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKCLASSES)) {
            if (!tweaker.endsWith(tweakerName)) continue;
            return true;
        }
        return false;
    }

    private static List<String> getIgnoredMods(Class<?> clCoreModManager) throws IllegalAccessException, InvocationTargetException {
        Method mdGetIgnoredMods = null;
        try {
            mdGetIgnoredMods = clCoreModManager.getDeclaredMethod(GlobalProperties.getString(GlobalProperties.Keys.FML_GET_IGNORED_MODS, "getIgnoredMods"), new Class[0]);
        }
        catch (NoSuchMethodException ex1) {
            try {
                mdGetIgnoredMods = clCoreModManager.getDeclaredMethod("getLoadedCoremods", new Class[0]);
            }
            catch (NoSuchMethodException ex2) {
                MixinPlatformAgentAbstract.logger.catching(org.spongepowered.asm.logging.Level.DEBUG, ex2);
                return Collections.emptyList();
            }
        }
        return (List)mdGetIgnoredMods.invoke(null, new Object[0]);
    }

    @Override
    public void init() {
        if (this.getCoreModManagerClass() != null) {
            this.injectRemapper();
        }
    }

    private void injectRemapper() {
        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        try {
            MixinPlatformAgentAbstract.logger.debug("Creating FML remapper adapter: {}", "org.spongepowered.asm.bridge.RemapperAdapterFML");
            Class<?> clFmlRemapperAdapter = Class.forName("org.spongepowered.asm.bridge.RemapperAdapterFML", true, (ClassLoader)Launch.classLoader);
            Method mdCreate = clFmlRemapperAdapter.getDeclaredMethod("create", new Class[0]);
            IRemapper remapper = (IRemapper)mdCreate.invoke(null, new Object[0]);
            env.getRemappers().add(remapper);
        }
        catch (Exception ex) {
            MixinPlatformAgentAbstract.logger.debug("Failed instancing FML remapper adapter, things will probably go horribly for notch-obf'd mods!", new Object[0]);
        }
        if (env.getOption(MixinEnvironment.Option.REFMAP_REMAP)) {
            env.getRemappers().add(new FMLLegacyDevRemapper(env));
        }
    }

    @Override
    public String getSideName() {
        List tweakerList = (List)GlobalProperties.get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKS);
        if (tweakerList == null) {
            return null;
        }
        for (ITweaker tweaker : tweakerList) {
            if (tweaker.getClass().getName().endsWith(".common.launcher.FMLServerTweaker")) {
                return "SERVER";
            }
            if (!tweaker.getClass().getName().endsWith(".common.launcher.FMLTweaker")) continue;
            return "CLIENT";
        }
        String name = MixinPlatformAgentAbstract.invokeStringMethod((ClassLoader)Launch.classLoader, "net.minecraftforge.fml.relauncher.FMLLaunchHandler", "side");
        if (name != null) {
            return name;
        }
        return MixinPlatformAgentAbstract.invokeStringMethod((ClassLoader)Launch.classLoader, "cpw.mods.fml.relauncher.FMLLaunchHandler", "side");
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return null;
    }

    @Override
    @Deprecated
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
        super.wire(phase, phaseConsumer);
        if (phase == MixinEnvironment.Phase.PREINIT) {
            MixinPlatformAgentFMLLegacy.begin(phaseConsumer);
        }
    }

    @Override
    @Deprecated
    public void unwire() {
        MixinPlatformAgentFMLLegacy.end();
    }

    static void begin(IConsumer<MixinEnvironment.Phase> delegate) {
        Logger fmlLog = LogManager.getLogger((String)"FML");
        if (!(fmlLog instanceof org.apache.logging.log4j.core.Logger)) {
            return;
        }
        log = (org.apache.logging.log4j.core.Logger)fmlLog;
        oldLevel = log.getLevel();
        appender = new MixinAppender(delegate);
        appender.start();
        log.addAppender((Appender)appender);
        log.setLevel(Level.ALL);
    }

    static void end() {
        if (log != null) {
            log.removeAppender((Appender)appender);
        }
    }

    static {
        for (String cmdLineCoreMod : System.getProperty("fml.coreMods.load", "").split(",")) {
            if (cmdLineCoreMod.isEmpty()) continue;
            MixinPlatformAgentAbstract.logger.debug("FML platform agent will ignore coremod {} specified on the command line", cmdLineCoreMod);
            loadedCoreMods.add(cmdLineCoreMod);
        }
        oldLevel = null;
    }

    static class MixinAppender
    extends AbstractAppender {
        private final IConsumer<MixinEnvironment.Phase> delegate;

        MixinAppender(IConsumer<MixinEnvironment.Phase> delegate) {
            super("MixinLogWatcherAppender", null, null);
            this.delegate = delegate;
        }

        public void append(LogEvent event) {
            if (event.getLevel() != Level.DEBUG || !"Validating minecraft".equals(event.getMessage().getFormattedMessage())) {
                return;
            }
            this.delegate.accept(MixinEnvironment.Phase.INIT);
            if (log.getLevel() == Level.ALL) {
                log.setLevel(oldLevel);
            }
        }
    }
}

