package org.spongepowered.tools.obfuscation.ext;

import java.util.HashSet;
import java.util.Set;

public final class SpecialPackages {
    private static final Set<String> suppressWarningsForPackages = new HashSet<String>();

    private SpecialPackages() {
    }

    public static final void addExcludedPackage(String packageName) {
        String internalName = packageName.replace('.', '/');
        if (!internalName.endsWith("/")) {
            internalName = internalName + "/";
        }
        suppressWarningsForPackages.add(internalName);
    }

    public static boolean isExcludedPackage(String internalName) {
        for (String prefix : suppressWarningsForPackages) {
            if (!internalName.startsWith(prefix)) continue;
            return true;
        }
        return false;
    }

    static {
        SpecialPackages.addExcludedPackage("java.");
        SpecialPackages.addExcludedPackage("javax.");
        SpecialPackages.addExcludedPackage("sun.");
        SpecialPackages.addExcludedPackage("com.sun.");
    }
}

