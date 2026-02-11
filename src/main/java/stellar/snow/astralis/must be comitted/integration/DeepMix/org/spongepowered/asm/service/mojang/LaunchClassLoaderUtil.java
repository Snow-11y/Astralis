package org.spongepowered.asm.service.mojang;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.service.IClassTracker;

final class LaunchClassLoaderUtil
implements IClassTracker {
    private final LaunchClassLoader classLoader;
    private final Map<String, Class<?>> cachedClasses;
    private final Set<String> invalidClasses;
    private final Set<String> classLoaderExceptions;
    private final Set<String> transformerExceptions;

    LaunchClassLoaderUtil(LaunchClassLoader classLoader) {
        this.classLoader = classLoader;
        this.cachedClasses = (Map)LaunchClassLoaderUtil.getField(classLoader, "cachedClasses");
        this.invalidClasses = (Set)LaunchClassLoaderUtil.getField(classLoader, "invalidClasses");
        this.classLoaderExceptions = (Set)LaunchClassLoaderUtil.getField(classLoader, "classLoaderExceptions");
        this.transformerExceptions = (Set)LaunchClassLoaderUtil.getField(classLoader, "transformerExceptions");
    }

    @Override
    public boolean isClassLoaded(String name) {
        return this.cachedClasses.containsKey(name);
    }

    @Override
    public String getClassRestrictions(String className) {
        String restrictions = "";
        if (this.isClassClassLoaderExcluded(className, null)) {
            restrictions = "PACKAGE_CLASSLOADER_EXCLUSION";
        }
        if (this.isClassTransformerExcluded(className, null)) {
            restrictions = (restrictions.length() > 0 ? restrictions + "," : "") + "PACKAGE_TRANSFORMER_EXCLUSION";
        }
        return restrictions;
    }

    boolean isClassExcluded(String name, String transformedName) {
        return this.isClassClassLoaderExcluded(name, transformedName) || this.isClassTransformerExcluded(name, transformedName);
    }

    boolean isClassClassLoaderExcluded(String name, String transformedName) {
        for (String exception : this.getClassLoaderExceptions()) {
            if ((transformedName == null || !transformedName.startsWith(exception)) && !name.startsWith(exception)) continue;
            return true;
        }
        return false;
    }

    boolean isClassTransformerExcluded(String name, String transformedName) {
        for (String exception : this.getTransformerExceptions()) {
            if ((transformedName == null || !transformedName.startsWith(exception)) && !name.startsWith(exception)) continue;
            return true;
        }
        return false;
    }

    @Override
    public void registerInvalidClass(String name) {
        if (this.invalidClasses != null) {
            this.invalidClasses.add(name);
        }
    }

    Set<String> getClassLoaderExceptions() {
        if (this.classLoaderExceptions != null) {
            return this.classLoaderExceptions;
        }
        return Collections.emptySet();
    }

    Set<String> getTransformerExceptions() {
        if (this.transformerExceptions != null) {
            return this.transformerExceptions;
        }
        return Collections.emptySet();
    }

    private static <T> T getField(LaunchClassLoader classLoader, String fieldName) {
        try {
            Field field = LaunchClassLoader.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T)field.get(classLoader);
        }
        catch (Exception exception) {
            return null;
        }
    }
}

