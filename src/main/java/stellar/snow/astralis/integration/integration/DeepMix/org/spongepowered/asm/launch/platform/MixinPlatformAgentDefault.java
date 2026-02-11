package org.spongepowered.asm.launch.platform;

import org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract;

public class MixinPlatformAgentDefault
extends MixinPlatformAgentAbstract {
    @Override
    public void prepare() {
        String connectorClass;
        String tokenProviders;
        String mixinConfigs;
        String compatibilityLevel = this.handle.getAttribute("MixinCompatibilityLevel");
        if (compatibilityLevel != null) {
            this.manager.setCompatibilityLevel(compatibilityLevel);
        }
        if ((mixinConfigs = this.handle.getAttribute("MixinConfigs")) != null) {
            for (String config : mixinConfigs.split(",")) {
                this.manager.addConfig(config.trim(), this.handle);
            }
        }
        if ((tokenProviders = this.handle.getAttribute("MixinTokenProviders")) != null) {
            for (String provider : tokenProviders.split(",")) {
                this.manager.addTokenProvider(provider.trim());
            }
        }
        if ((connectorClass = this.handle.getAttribute("MixinConnector")) != null) {
            this.manager.addConnector(connectorClass.trim());
        }
    }
}

