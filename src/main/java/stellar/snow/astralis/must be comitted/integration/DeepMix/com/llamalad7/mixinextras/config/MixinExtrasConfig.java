package com.llamalad7.mixinextras.config;

import com.llamalad7.mixinextras.lib.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.lib.semver.Version;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

public class MixinExtrasConfig {
    @SerializedName(value="minVersion")
    private final String minVersionString;
    private final transient String configName;
    public final transient MixinExtrasVersion minVersion;

    public MixinExtrasConfig(IMixinConfig config, String minVersion) {
        this.configName = config.getName();
        this.minVersionString = minVersion;
        this.minVersion = this.determineMinVersion();
    }

    private MixinExtrasVersion determineMinVersion() {
        if (this.minVersionString == null) {
            return null;
        }
        Version min = Version.tryParse(this.minVersionString).orElseThrow(() -> new IllegalArgumentException(String.format("'%s' is not valid SemVer!", this.minVersionString)));
        MixinExtrasVersion[] versions = MixinExtrasVersion.values();
        if (min.isHigherThan(MixinExtrasVersion.LATEST.getSemver())) {
            throw new IllegalArgumentException(String.format("Mixin Config %s requires MixinExtras >=%s but %s is present!", new Object[]{this.configName, min, MixinExtrasVersion.LATEST}));
        }
        MixinExtrasVersion result = versions[0];
        for (MixinExtrasVersion version : versions) {
            if (version.getSemver().isHigherThan(min)) break;
            result = version;
        }
        return result;
    }
}

