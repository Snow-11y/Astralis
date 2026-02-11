package org.spongepowered.asm.util;

import java.util.Optional;

public abstract class ObfuscationUtil {
    private ObfuscationUtil() {
    }

    public static String mapDescriptor(String desc, IClassRemapper remapper) {
        return ObfuscationUtil.remapDescriptor(desc, remapper, false);
    }

    public static String unmapDescriptor(String desc, IClassRemapper remapper) {
        return ObfuscationUtil.remapDescriptor(desc, remapper, true);
    }

    private static String remapDescriptor(String desc, IClassRemapper remapper, boolean unmap) {
        StringBuilder sb = new StringBuilder();
        StringBuilder token = null;
        boolean remapped = false;
        for (int pos = 0; pos < desc.length(); ++pos) {
            char c = desc.charAt(pos);
            if (token != null) {
                if (c == ';') {
                    String tokenStr = token.toString();
                    Optional<String> remappedStr = ObfuscationUtil.remap(tokenStr, remapper, unmap);
                    if (remappedStr.isPresent()) {
                        remapped = true;
                    }
                    sb.append('L').append(remappedStr.orElse(tokenStr)).append(';');
                    token = null;
                    continue;
                }
                token.append(c);
                continue;
            }
            if (c == 'L') {
                token = new StringBuilder();
                continue;
            }
            sb.append(c);
        }
        if (token != null) {
            throw new IllegalArgumentException("Invalid descriptor '" + desc + "', missing ';'");
        }
        return remapped ? sb.toString() : null;
    }

    private static Optional<String> remap(String typeName, IClassRemapper remapper, boolean unmap) {
        String result = unmap ? remapper.unmap(typeName) : remapper.map(typeName);
        return Optional.ofNullable(result);
    }

    public static interface IClassRemapper {
        public String map(String var1);

        public String unmap(String var1);
    }
}

