package com.llamalad7.mixinextras.sugar.impl.handlers;

import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.util.Bytecode;

public class HandlerInfo {
    private final Map<Integer, ParameterWrapper> wrappers = new LinkedHashMap<Integer, ParameterWrapper>();

    public void wrapParameter(SugarParameter param, Type type, Type generic, BiConsumer<InsnList, Runnable> unwrap) {
        this.wrappers.put(param.paramIndex, new ParameterWrapper(type, generic, unwrap));
    }

    public void transformHandler(ClassNode targetClass, MethodNode handler) {
        Type[] paramTypes = Type.getArgumentTypes((String)handler.desc);
        InsnList insns = new InsnList();
        if (!Bytecode.isStatic(handler)) {
            insns.add((AbstractInsnNode)new VarInsnNode(25, 0));
        }
        int index = Bytecode.isStatic(handler) ? 0 : 1;
        for (int i = 0; i < paramTypes.length; ++i) {
            VarInsnNode loadInsn = new VarInsnNode(paramTypes[i].getOpcode(21), index);
            ParameterWrapper wrapper = this.wrappers.get(i);
            if (wrapper != null) {
                paramTypes[i] = wrapper.type;
                loadInsn.setOpcode(wrapper.type.getOpcode(21));
                wrapper.unwrap.accept(insns, () -> insns.add((AbstractInsnNode)loadInsn));
            } else {
                insns.add((AbstractInsnNode)loadInsn);
            }
            index += paramTypes[i].getSize();
        }
        insns.add((AbstractInsnNode)ASMUtils.getInvokeInstruction(targetClass, handler));
        insns.add((AbstractInsnNode)new InsnNode(Type.getReturnType((String)handler.desc).getOpcode(172)));
        handler.instructions = insns;
        handler.localVariables = null;
        handler.name = UniquenessHelper.getUniqueMethodName(targetClass, handler.name + "$mixinextras$bridge");
        handler.desc = Type.getMethodDescriptor((Type)Type.getReturnType((String)handler.desc), (Type[])paramTypes);
    }

    public void transformGenerics(ArrayList<Type> generics) {
        for (Map.Entry<Integer, ParameterWrapper> entry : this.wrappers.entrySet()) {
            Type type = entry.getValue().generic;
            generics.set(entry.getKey(), type);
        }
    }

    private static class ParameterWrapper {
        private final Type type;
        private final Type generic;
        private final BiConsumer<InsnList, Runnable> unwrap;

        private ParameterWrapper(Type type, Type generic, BiConsumer<InsnList, Runnable> unwrap) {
            this.type = type;
            this.generic = generic;
            this.unwrap = unwrap;
        }
    }
}

