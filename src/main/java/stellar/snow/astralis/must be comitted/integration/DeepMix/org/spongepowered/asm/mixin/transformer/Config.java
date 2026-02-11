package org.spongepowered.asm.mixin.transformer;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;
import org.spongepowered.asm.mixin.transformer.MixinConfig;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.include.com.google.common.base.Strings;

public class Config {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private static final Map<String, Config> allConfigs = new HashMap<String, Config>();
    private final String name;
    private final MixinConfig config;

    public Config(MixinConfig config) {
        this.name = config.getName();
        this.config = config;
    }

    public String getName() {
        return this.name;
    }

    MixinConfig get() {
        return this.config;
    }

    public boolean isVisited() {
        return this.config.isVisited();
    }

    public IMixinConfig getConfig() {
        return this.config;
    }

    public MixinEnvironment getEnvironment() {
        return this.config.getEnvironment();
    }

    public Config getParent() {
        MixinConfig parent = this.config.getParent();
        return parent != null ? parent.getHandle() : null;
    }

    public String toString() {
        return this.config.toString();
    }

    public boolean equals(Object obj) {
        return obj instanceof Config && this.name.equals(((Config)obj).name);
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    @Deprecated
    public static Config create(String configFile, MixinEnvironment outer) {
        return Config.create(configFile, outer, null);
    }

    @Deprecated
    public static Config create(String configFile, MixinEnvironment outer, IMixinConfigSource source) {
        Config config = allConfigs.get(configFile);
        if (config != null) {
            return config;
        }
        try {
            config = MixinConfig.create(configFile, outer, source);
            if (config != null) {
                allConfigs.put(config.getName(), config);
            }
        }
        catch (Exception ex) {
            throw new MixinInitialisationError("Error initialising mixin config " + configFile, ex);
        }
        if (config == null) {
            return null;
        }
        String parent = config.get().getParentName();
        if (!Strings.isNullOrEmpty(parent)) {
            Config parentConfig;
            try {
                parentConfig = Config.create(parent, outer, source);
                if (parentConfig != null && !config.get().assignParent(parentConfig)) {
                    config = null;
                }
            }
            catch (Throwable th) {
                throw new MixinInitialisationError("Error initialising parent mixin config " + parent + " of " + configFile, th);
            }
            if (parentConfig == null) {
                logger.error("Error encountered initialising mixin config {0}: The parent {1} could not be read.", configFile, parent);
            }
        }
        return config;
    }

    public static Config create(String configFile, IMixinConfigSource source) {
        return MixinConfig.create(configFile, MixinEnvironment.getDefaultEnvironment(), source);
    }
}

