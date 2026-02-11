package org.spongepowered.tools.obfuscation.mapping.fg3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingProvider;

public class MappingMethodLazy
extends MappingMethod {
    private static final Pattern PATTERN_CLASSNAME = Pattern.compile("L([^;]+);");
    private final String originalDesc;
    private final IMappingProvider mappingProvider;
    private String newDesc;

    public MappingMethodLazy(String owner, String simpleName, String originalDesc, IMappingProvider mappingProvider) {
        super(owner, simpleName, "{" + originalDesc + "}");
        this.originalDesc = originalDesc;
        this.mappingProvider = mappingProvider;
    }

    @Override
    public String getDesc() {
        if (this.newDesc == null) {
            this.newDesc = this.generateDescriptor();
        }
        return this.newDesc;
    }

    @Override
    public String toString() {
        String desc = this.getDesc();
        return String.format("%s%s%s", this.getName(), desc != null ? " " : "", desc != null ? desc : "");
    }

    private String generateDescriptor() {
        StringBuffer desc = new StringBuffer();
        Matcher matcher = PATTERN_CLASSNAME.matcher(this.originalDesc);
        while (matcher.find()) {
            String remapped = this.mappingProvider.getClassMapping(matcher.group(1));
            if (remapped != null) {
                matcher.appendReplacement(desc, Matcher.quoteReplacement("L" + remapped + ";"));
                continue;
            }
            matcher.appendReplacement(desc, Matcher.quoteReplacement("L" + matcher.group(1) + ";"));
        }
        matcher.appendTail(desc);
        return desc.toString();
    }
}

