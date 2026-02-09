package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.config.MixinExtrasConfig;
import com.llamalad7.mixinextras.lib.gson.Strictness;
import com.llamalad7.mixinextras.lib.gson.stream.JsonReader;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.WeakHashMap;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.service.MixinService;

public class MixinConfigUtils {
    private static final String KEY_TOP_LEVEL_MIN_VERSION = "minMixinExtrasVersion";
    private static final String KEY_SUBCONFIG = "mixinextras";
    private static final String KEY_MIN_VERSION = "minVersion";
    private static final Map<IMixinConfig, MixinExtrasConfig> CONFIG_CACHE = new WeakHashMap<IMixinConfig, MixinExtrasConfig>();

    public static void requireMinVersion(IMixinConfig config, MixinExtrasVersion desiredVersion, String featureName) {
        MixinExtrasVersion min = MixinConfigUtils.extraConfigFor((IMixinConfig)config).minVersion;
        if (min == null || min.getNumber() < desiredVersion.getNumber()) {
            throw new UnsupportedOperationException(String.format("In order to use %s, Mixin Config '%s' needs to declare a reliance on MixinExtras >=%s! E.g. `\"%s\": {\"%s\": \"%s\"}`", new Object[]{featureName, config, desiredVersion, KEY_SUBCONFIG, KEY_MIN_VERSION, MixinExtrasVersion.LATEST}));
        }
    }

    private static MixinExtrasConfig extraConfigFor(IMixinConfig config) {
        return CONFIG_CACHE.computeIfAbsent(config, k -> new MixinExtrasConfig(config, MixinConfigUtils.readMinString(config)));
    }

    private static String readMinString(IMixinConfig config) {
        return MixinConfigUtils.readConfig(config, reader -> {
            reader.beginObject();
            block8: while (reader.hasNext()) {
                String key;
                switch (key = reader.nextName()) {
                    case "mixinextras": {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String innerKey = reader.nextName();
                            if (innerKey.equals(KEY_MIN_VERSION)) {
                                return reader.nextString();
                            }
                            reader.skipValue();
                        }
                        reader.endObject();
                        continue block8;
                    }
                    case "minMixinExtrasVersion": {
                        return reader.nextString();
                    }
                }
                reader.skipValue();
            }
            return null;
        });
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static <T> T readConfig(IMixinConfig config, JsonProcessor<T> compute) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(MixinService.getService().getResourceAsStream(config.getName())));){
            reader.setStrictness(Strictness.LENIENT);
            T t = compute.process(reader);
            return t;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to read mixin config " + config.getName(), e);
        }
    }

    @FunctionalInterface
    private static interface JsonProcessor<T> {
        public T process(JsonReader var1) throws IOException;
    }
}

