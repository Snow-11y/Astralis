package org.spongepowered.asm.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterDefault;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public abstract class MixinServiceAbstract
implements IMixinService {
    protected static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
    protected static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";
    protected static final String SERVICE_PACKAGE = "org.spongepowered.asm.service.";
    private static final Map<String, ILogger> loggers = new HashMap<String, ILogger>();
    protected final ReEntranceLock lock = new ReEntranceLock(1);
    private final Map<Class<IMixinInternal>, IMixinInternal> internals = new HashMap<Class<IMixinInternal>, IMixinInternal>();
    private List<IMixinPlatformServiceAgent> serviceAgents;
    private String sideName;

    @Override
    public void prepare() {
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return null;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return null;
    }

    @Override
    public void offer(IMixinInternal internal) {
        this.registerInternal(internal, internal.getClass());
    }

    private void registerInternal(IMixinInternal internal, Class<?> clazz) {
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface == IMixinInternal.class) {
                this.internals.put(clazz, internal);
            }
            this.registerInternal(internal, iface);
        }
    }

    protected final <T extends IMixinInternal> T getInternal(Class<T> type) {
        for (Class<IMixinInternal> internalType : this.internals.keySet()) {
            if (!type.isAssignableFrom(internalType)) continue;
            return (T)this.internals.get(internalType);
        }
        return null;
    }

    @Override
    public void init() {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.init();
        }
    }

    @Override
    public void beginPhase() {
    }

    @Override
    public void checkEnv(Object bootSource) {
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return this.lock;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        ImmutableList.Builder<IContainerHandle> list = ImmutableList.builder();
        this.getContainersFromAgents(list);
        return list.build();
    }

    protected final void getContainersFromAgents(ImmutableList.Builder<IContainerHandle> list) {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            Collection<IContainerHandle> containers = agent.getMixinContainers();
            if (containers == null) continue;
            list.addAll(containers);
        }
    }

    @Override
    public final String getSideName() {
        if (this.sideName != null) {
            return this.sideName;
        }
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            try {
                String side = agent.getSideName();
                if (side == null) continue;
                this.sideName = side;
                return this.sideName;
            }
            catch (Exception ex) {
                this.getLogger("mixin").catching(ex);
            }
        }
        return "UNKNOWN";
    }

    private List<IMixinPlatformServiceAgent> getServiceAgents() {
        if (this.serviceAgents != null) {
            return this.serviceAgents;
        }
        this.serviceAgents = new ArrayList<IMixinPlatformServiceAgent>();
        for (String agentClassName : this.getPlatformAgents()) {
            try {
                Class<?> agentClass = this.getClassProvider().findClass(agentClassName, false);
                IMixinPlatformAgent agent = (IMixinPlatformAgent)agentClass.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
                if (!(agent instanceof IMixinPlatformServiceAgent)) continue;
                this.serviceAgents.add((IMixinPlatformServiceAgent)agent);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return this.serviceAgents;
    }

    @Override
    public synchronized ILogger getLogger(String name) {
        ILogger logger = loggers.get(name);
        if (logger == null) {
            logger = this.createLogger(name);
            loggers.put(name, logger);
        }
        return logger;
    }

    protected ILogger createLogger(String name) {
        return new LoggerAdapterDefault(name);
    }

    @Deprecated
    public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.wire(phase, phaseConsumer);
        }
    }

    @Deprecated
    public void unwire() {
        for (IMixinPlatformServiceAgent agent : this.getServiceAgents()) {
            agent.unwire();
        }
    }
}

