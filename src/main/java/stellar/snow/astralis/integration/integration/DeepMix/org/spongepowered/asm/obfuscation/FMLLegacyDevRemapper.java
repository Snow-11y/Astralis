package org.spongepowered.asm.obfuscation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.include.com.google.common.base.Charsets;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.io.Files;
import org.spongepowered.include.com.google.common.io.LineProcessor;

public class FMLLegacyDevRemapper
implements IRemapper {
    private static final Logger logger = LogManager.getLogger((String)"mixin");
    private static final Map<String, Map<String, String>> srgs = new HashMap<String, Map<String, String>>();
    private final Map<String, String> mappings;
    private final Map<String, String> descCache = new HashMap<String, String>();

    public FMLLegacyDevRemapper(MixinEnvironment env) {
        String resource = FMLLegacyDevRemapper.getResource(env);
        this.mappings = FMLLegacyDevRemapper.loadSrgs(resource);
        logger.info("Initialised Legacy FML Dev Remapper");
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        return this.mappings.getOrDefault(name, name);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        return this.mappings.getOrDefault(name, name);
    }

    @Override
    public String map(String typeName) {
        return typeName;
    }

    @Override
    public String unmap(String typeName) {
        return typeName;
    }

    @Override
    public String mapDesc(String desc) {
        String remapped = this.descCache.get(desc);
        if (remapped == null) {
            remapped = desc;
            for (Map.Entry<String, String> entry : this.mappings.entrySet()) {
                remapped = remapped.replace(entry.getKey(), entry.getValue());
            }
            this.descCache.put(desc, remapped);
        }
        return remapped;
    }

    @Override
    public String unmapDesc(String desc) {
        throw new UnsupportedOperationException();
    }

    private static Map<String, String> loadSrgs(String fileName) {
        if (srgs.containsKey(fileName)) {
            return srgs.get(fileName);
        }
        final HashMap<String, String> map = new HashMap<String, String>();
        srgs.put(fileName, map);
        File file = new File(fileName);
        if (!file.isFile()) {
            return map;
        }
        try {
            Files.readLines(file, Charsets.UTF_8, new LineProcessor<Object>(){

                @Override
                public Object getResult() {
                    return null;
                }

                @Override
                public boolean processLine(String line) throws IOException {
                    if (Strings.isNullOrEmpty(line) || line.startsWith("#")) {
                        return true;
                    }
                    int fromPos = 0;
                    int toPos = 0;
                    if ((line.startsWith("MD: ") ? 2 : (toPos = line.startsWith("FD: ") ? 1 : 0)) > 0) {
                        String[] entries = line.substring(4).split(" ", 4);
                        map.put(entries[fromPos].substring(entries[fromPos].lastIndexOf(47) + 1), entries[toPos].substring(entries[toPos].lastIndexOf(47) + 1));
                    }
                    return true;
                }
            });
        }
        catch (IOException ex) {
            logger.warn("Could not read input SRG file: {}", new Object[]{fileName});
            logger.catching((Throwable)ex);
        }
        return map;
    }

    private static String getResource(MixinEnvironment env) {
        String resource = env.getOptionValue(MixinEnvironment.Option.REFMAP_REMAP_RESOURCE);
        return Strings.isNullOrEmpty(resource) ? System.getProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp") : resource;
    }
}

