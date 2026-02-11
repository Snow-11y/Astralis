package org.spongepowered.asm.launch.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

public class MixinContainer {
    private static final List<String> agentClasses = new ArrayList<String>();
    private static final ILogger logger;
    private final IContainerHandle handle;
    private final List<IMixinPlatformAgent> agents = new ArrayList<IMixinPlatformAgent>();

    public MixinContainer(MixinPlatformManager manager, IContainerHandle handle) {
        this.handle = handle;
        Iterator<String> iter = agentClasses.iterator();
        while (iter.hasNext()) {
            String agentClass = iter.next();
            try {
                Class<?> clazz = Class.forName(agentClass);
                String simpleName = clazz.getSimpleName();
                logger.debug("Instancing new {} for {}", simpleName, this.handle);
                IMixinPlatformAgent agent = (IMixinPlatformAgent)clazz.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
                IMixinPlatformAgent.AcceptResult acceptAction = agent.accept(manager, this.handle);
                if (acceptAction == IMixinPlatformAgent.AcceptResult.ACCEPTED) {
                    this.agents.add(agent);
                } else if (acceptAction == IMixinPlatformAgent.AcceptResult.INVALID) {
                    iter.remove();
                    continue;
                }
                logger.debug("{} {} container {}", simpleName, acceptAction.name().toLowerCase(Locale.ROOT), this.handle);
            }
            catch (InstantiationException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                }
                throw new RuntimeException(cause);
            }
            catch (ReflectiveOperationException ex) {
                logger.catching(ex);
            }
        }
    }

    public IContainerHandle getDescriptor() {
        return this.handle;
    }

    public Collection<String> getPhaseProviders() {
        ArrayList<String> phaseProviders = new ArrayList<String>();
        for (IMixinPlatformAgent agent : this.agents) {
            String phaseProvider = agent.getPhaseProvider();
            if (phaseProvider == null) continue;
            phaseProviders.add(phaseProvider);
        }
        return phaseProviders;
    }

    public void prepare() {
        for (IMixinPlatformAgent agent : this.agents) {
            logger.debug("Processing prepare() for {}", agent);
            agent.prepare();
        }
    }

    public void initPrimaryContainer() {
        for (IMixinPlatformAgent agent : this.agents) {
            logger.debug("Processing launch tasks for {}", agent);
            agent.initPrimaryContainer();
        }
    }

    public void inject() {
        for (IMixinPlatformAgent agent : this.agents) {
            logger.debug("Processing inject() for {}", agent);
            agent.inject();
        }
    }

    static {
        GlobalProperties.put(GlobalProperties.Keys.AGENTS, agentClasses);
        for (String agent : MixinService.getService().getPlatformAgents()) {
            agentClasses.add(agent);
        }
        agentClasses.add("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
        logger = MixinService.getService().getLogger("mixin");
    }
}

