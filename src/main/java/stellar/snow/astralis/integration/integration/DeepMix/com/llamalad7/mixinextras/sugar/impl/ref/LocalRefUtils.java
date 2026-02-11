package com.llamalad7.mixinextras.sugar.impl.ref;

import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefClassGenerator;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalByteRef;
import com.llamalad7.mixinextras.sugar.ref.LocalCharRef;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.llamalad7.mixinextras.sugar.ref.LocalShortRef;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class LocalRefUtils {
    public static Class<?> getInterfaceFor(Type type) {
        if (!ASMUtils.isPrimitive(type)) {
            return LocalRef.class;
        }
        switch (type.getDescriptor().charAt(0)) {
            case 'Z': {
                return LocalBooleanRef.class;
            }
            case 'B': {
                return LocalByteRef.class;
            }
            case 'C': {
                return LocalCharRef.class;
            }
            case 'S': {
                return LocalShortRef.class;
            }
            case 'I': {
                return LocalIntRef.class;
            }
            case 'J': {
                return LocalLongRef.class;
            }
            case 'F': {
                return LocalFloatRef.class;
            }
            case 'D': {
                return LocalDoubleRef.class;
            }
        }
        throw new IllegalStateException("Illegal descriptor " + type.getDescriptor());
    }

    public static Type getTargetType(Type type, Type generic) {
        if (type.getSort() != 10 || !MixinExtrasService.getInstance().isClassOwned(type.getClassName())) {
            return type;
        }
        switch (StringUtils.substringAfterLast(type.getInternalName(), "/")) {
            case "LocalBooleanRef": {
                return Type.BOOLEAN_TYPE;
            }
            case "LocalByteRef": {
                return Type.BYTE_TYPE;
            }
            case "LocalCharRef": {
                return Type.CHAR_TYPE;
            }
            case "LocalDoubleRef": {
                return Type.DOUBLE_TYPE;
            }
            case "LocalFloatRef": {
                return Type.FLOAT_TYPE;
            }
            case "LocalIntRef": {
                return Type.INT_TYPE;
            }
            case "LocalLongRef": {
                return Type.LONG_TYPE;
            }
            case "LocalShortRef": {
                return Type.SHORT_TYPE;
            }
            case "LocalRef": {
                if (generic == null) {
                    throw new IllegalStateException("LocalRef must have a concrete type argument!");
                }
                return generic;
            }
        }
        return type;
    }

    public static void generateNew(InsnList insns, Type innerType) {
        String refImpl = LocalRefClassGenerator.getForType(innerType);
        insns.add((AbstractInsnNode)new TypeInsnNode(187, refImpl));
        insns.add((AbstractInsnNode)new InsnNode(89));
        insns.add((AbstractInsnNode)new MethodInsnNode(183, refImpl, "<init>", "()V", false));
    }

    public static void generateInitialization(InsnList insns, Type innerType) {
        String refImpl = LocalRefClassGenerator.getForType(innerType);
        insns.add((AbstractInsnNode)new MethodInsnNode(182, refImpl, "init", Type.getMethodDescriptor((Type)Type.VOID_TYPE, (Type[])new Type[]{LocalRefUtils.getErasedType(innerType)}), false));
    }

    public static void generateDisposal(InsnList insns, Type innerType) {
        String refImpl = LocalRefClassGenerator.getForType(innerType);
        insns.add((AbstractInsnNode)new MethodInsnNode(182, refImpl, "dispose", Type.getMethodDescriptor((Type)LocalRefUtils.getErasedType(innerType), (Type[])new Type[0]), false));
        if (!ASMUtils.isPrimitive(innerType)) {
            insns.add((AbstractInsnNode)new TypeInsnNode(192, innerType.getInternalName()));
        }
    }

    public static void generateUnwrapping(InsnList insns, Type innerType, Runnable load) {
        String refInterface = Type.getInternalName(LocalRefUtils.getInterfaceFor(innerType));
        load.run();
        insns.add((AbstractInsnNode)new MethodInsnNode(185, refInterface, "get", Type.getMethodDescriptor((Type)LocalRefUtils.getErasedType(innerType), (Type[])new Type[0]), true));
        if (!ASMUtils.isPrimitive(innerType)) {
            insns.add((AbstractInsnNode)new TypeInsnNode(192, innerType.getInternalName()));
        }
    }

    private static Type getErasedType(Type actual) {
        return ASMUtils.isPrimitive(actual) ? actual : ASMUtils.OBJECT_TYPE;
    }
}

