package org.spongepowered.asm.service;

import java.io.InputStream;
import java.util.Collection;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.util.ReEntranceLock;

public interface IMixinService {
    public String getName();

    public boolean isValid();

    public void prepare();

    public MixinEnvironment.Phase getInitialPhase();

    public void offer(IMixinInternal var1);

    public void init();

    public void beginPhase();

    public void checkEnv(Object var1);

    public ReEntranceLock getReEntranceLock();

    public IClassProvider getClassProvider();

    public IClassBytecodeProvider getBytecodeProvider();

    public ITransformerProvider getTransformerProvider();

    public IClassTracker getClassTracker();

    public IMixinAuditTrail getAuditTrail();

    public Collection<String> getPlatformAgents();

    public IContainerHandle getPrimaryContainer();

    public Collection<IContainerHandle> getMixinContainers();

    public InputStream getResourceAsStream(String var1);

    public String getSideName();

    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel();

    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel();

    public ILogger getLogger(String var1);
}

