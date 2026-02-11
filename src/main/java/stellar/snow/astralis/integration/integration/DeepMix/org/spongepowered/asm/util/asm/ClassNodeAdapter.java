package org.spongepowered.asm.util.asm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.util.asm.ASM;

public final class ClassNodeAdapter {
    private static final Field fdNestHost = ClassNodeAdapter.getField("nestHostClass");
    private static final Field fdNestMembers = ClassNodeAdapter.getField("nestMembers");
    private static boolean notSupported = false;

    private ClassNodeAdapter() {
    }

    public static String getNestHostClass(ClassNode classNode) {
        if (ASM.isAtLeastVersion(7)) {
            return classNode.nestHostClass;
        }
        if (fdNestHost == null || notSupported) {
            return null;
        }
        try {
            return (String)fdNestHost.get(classNode);
        }
        catch (ReflectiveOperationException ex) {
            notSupported = true;
            return null;
        }
    }

    public static void setNestHostClass(ClassNode classNode, String nestHostClass) {
        if (ASM.isAtLeastVersion(7)) {
            classNode.nestHostClass = nestHostClass;
        }
        if (fdNestHost == null || notSupported) {
            return;
        }
        try {
            fdNestHost.set(classNode, nestHostClass);
        }
        catch (ReflectiveOperationException ex) {
            notSupported = true;
        }
    }

    public static List<String> getNestMembers(ClassNode classNode) {
        if (ASM.isAtLeastVersion(7)) {
            return classNode.nestMembers;
        }
        if (fdNestMembers == null || notSupported) {
            return null;
        }
        try {
            return (List)fdNestMembers.get(classNode);
        }
        catch (ReflectiveOperationException ex) {
            notSupported = true;
            return null;
        }
    }

    public static List<String> getNestMembersAsList(ClassNode classNode) {
        List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
        if (nestMembers == null) {
            nestMembers = new ArrayList<String>();
            ClassNodeAdapter.setNestMembers(classNode, nestMembers);
        }
        return nestMembers;
    }

    public static void setNestMembers(ClassNode classNode, List<String> nestMembers) {
        if (ASM.isAtLeastVersion(7)) {
            classNode.nestMembers = nestMembers;
            return;
        }
        if (fdNestMembers == null || notSupported) {
            return;
        }
        try {
            fdNestMembers.set(classNode, nestMembers);
        }
        catch (ReflectiveOperationException ex) {
            notSupported = true;
        }
    }

    private static Field getField(String fieldBaseName) {
        try {
            return ClassNode.class.getDeclaredField(fieldBaseName);
        }
        catch (NoSuchFieldException ex) {
            try {
                return ClassNode.class.getDeclaredField(fieldBaseName + "Experimental");
            }
            catch (NoSuchFieldException ex1) {
                notSupported = true;
                return null;
            }
        }
    }
}

