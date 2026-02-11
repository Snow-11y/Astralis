package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.utils.MixinInternals;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import sun.misc.Unsafe;

public class ClassGenUtils {
    private static final Definer DEFINER;
    private static final Map<String, byte[]> DEFINITIONS;

    public static void defineClass(ClassNode node, MethodHandles.Lookup scope) {
        ClassWriter writer = new ClassWriter(2);
        node.accept((ClassVisitor)writer);
        byte[] bytes = writer.toByteArray();
        String name = node.name.replace('/', '.');
        try {
            DEFINER.define(name, bytes, scope);
        }
        catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to define class %s from %s! Please report to LlamaLad7!", node.name, scope), e);
        }
        DEFINITIONS.put(name, bytes);
        MixinInternals.registerClassInfo(node);
        MixinInternals.getExtensions().export(MixinEnvironment.getCurrentEnvironment(), node.name, false, node);
    }

    public static Map<String, byte[]> getDefinitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    static {
        Definer theDefiner;
        DEFINITIONS = new HashMap<String, byte[]>();
        try {
            Method defineClass = Unsafe.class.getMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE, ClassLoader.class, ProtectionDomain.class);
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe)theUnsafe.get(null);
            theDefiner = (name, bytes, scope) -> defineClass.invoke(unsafe, name, bytes, 0, bytes.length, scope.lookupClass().getClassLoader(), scope.lookupClass().getProtectionDomain());
        }
        catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException e1) {
            try {
                Method defineClass = MethodHandles.Lookup.class.getMethod("defineClass", byte[].class);
                theDefiner = (name, bytes, scope) -> defineClass.invoke(scope, new Object[]{bytes});
            }
            catch (NoSuchMethodException e2) {
                RuntimeException e = new RuntimeException("Could not resolve class definer! Please report to LlamaLad7.");
                e.addSuppressed(e1);
                e.addSuppressed(e2);
                throw e;
            }
        }
        DEFINER = theDefiner;
    }

    @FunctionalInterface
    private static interface Definer {
        public void define(String var1, byte[] var2, MethodHandles.Lookup var3) throws Throwable;
    }
}

