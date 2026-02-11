package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationRuntime;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;

public class OperationUtils {
    public static void makeOperation(Type[] argTypes, Type returnType, InsnList insns, boolean virtual, Type[] trailingParams, ClassNode classNode, Type operationType, String name, OperationContents contents) {
        Type[] descriptorArgs = trailingParams;
        if (virtual) {
            descriptorArgs = ArrayUtils.add(descriptorArgs, 0, Type.getObjectType((String)classNode.name));
        }
        insns.add((AbstractInsnNode)new InvokeDynamicInsnNode("call", Type.getMethodDescriptor((Type)operationType, (Type[])descriptorArgs), ASMUtils.LMF_HANDLE, new Object[]{Type.getMethodType((Type)Type.getType(Object.class), (Type[])new Type[]{Type.getType(Object[].class)}), OperationUtils.generateSyntheticBridge(argTypes, returnType, virtual, trailingParams, name, classNode, contents), Type.getMethodType((Type)(ASMUtils.isPrimitive(returnType) ? Type.getObjectType((String)(returnType == Type.VOID_TYPE ? "java/lang/Void" : Bytecode.getBoxingType(returnType))) : returnType), (Type[])new Type[]{Type.getType(Object[].class)})}));
    }

    private static Handle generateSyntheticBridge(final Type[] argTypes, final Type returnType, final boolean virtual, final Type[] boundParams, String name, ClassNode classNode, final OperationContents contents) {
        MethodNode method = new MethodNode(ASM.API_VERSION, 0x1002 | (virtual ? 0 : 8), UniquenessHelper.getUniqueMethodName(classNode, "mixinextras$bridge$" + name), Bytecode.generateDescriptor(ASMUtils.isPrimitive(returnType) ? Type.getObjectType((String)(returnType == Type.VOID_TYPE ? "java/lang/Void" : Bytecode.getBoxingType(returnType))) : returnType, ArrayUtils.add(boundParams, Type.getType(Object[].class))), null, null);
        method.instructions = new InsnList(){
            {
                int paramArrayIndex = Arrays.stream(boundParams).mapToInt(Type::getSize).sum() + (virtual ? 1 : 0);
                this.add((AbstractInsnNode)new VarInsnNode(25, paramArrayIndex));
                this.add((AbstractInsnNode)new IntInsnNode(16, argTypes.length));
                this.add((AbstractInsnNode)new LdcInsnNode((Object)Arrays.stream(argTypes).map(Type::getClassName).collect(Collectors.joining(", ", "[", "]"))));
                this.add((AbstractInsnNode)new MethodInsnNode(184, Type.getInternalName(WrapOperationRuntime.class), "checkArgumentCount", Bytecode.generateDescriptor(Void.TYPE, Object[].class, Integer.TYPE, String.class), false));
                if (virtual) {
                    this.add((AbstractInsnNode)new VarInsnNode(25, 0));
                }
                Consumer<InsnList> loadArgs = insns -> {
                    insns.add((AbstractInsnNode)new VarInsnNode(25, paramArrayIndex));
                    for (int i = 0; i < argTypes.length; ++i) {
                        Type argType = argTypes[i];
                        insns.add((AbstractInsnNode)new InsnNode(89));
                        insns.add((AbstractInsnNode)new IntInsnNode(16, i));
                        insns.add((AbstractInsnNode)new InsnNode(50));
                        if (ASMUtils.isPrimitive(argType)) {
                            insns.add((AbstractInsnNode)new TypeInsnNode(192, Bytecode.getBoxingType(argType)));
                            insns.add((AbstractInsnNode)new MethodInsnNode(182, Bytecode.getBoxingType(argType), Bytecode.getUnboxingMethod(argType), Type.getMethodDescriptor((Type)argType, (Type[])new Type[0]), false));
                        } else {
                            insns.add((AbstractInsnNode)new TypeInsnNode(192, argType.getInternalName()));
                        }
                        if (argType.getSize() == 2) {
                            insns.add((AbstractInsnNode)new InsnNode(93));
                            insns.add((AbstractInsnNode)new InsnNode(88));
                            continue;
                        }
                        insns.add((AbstractInsnNode)new InsnNode(95));
                    }
                    insns.add((AbstractInsnNode)new InsnNode(87));
                    int boundParamIndex = virtual ? 1 : 0;
                    for (Type boundParamType : boundParams) {
                        insns.add((AbstractInsnNode)new VarInsnNode(boundParamType.getOpcode(21), boundParamIndex));
                        boundParamIndex += boundParamType.getSize();
                    }
                };
                this.add(contents.generate(paramArrayIndex, loadArgs));
                if (returnType == Type.VOID_TYPE) {
                    this.add((AbstractInsnNode)new InsnNode(1));
                    this.add((AbstractInsnNode)new TypeInsnNode(192, "java/lang/Void"));
                } else if (ASMUtils.isPrimitive(returnType)) {
                    this.add((AbstractInsnNode)new MethodInsnNode(184, Bytecode.getBoxingType(returnType), "valueOf", Bytecode.generateDescriptor(Type.getObjectType((String)Bytecode.getBoxingType(returnType)), returnType), false));
                }
                this.add((AbstractInsnNode)new InsnNode(176));
            }
        };
        classNode.methods.add(method);
        return new Handle(virtual ? 7 : 6, classNode.name, method.name, method.desc, (classNode.access & 0x200) != 0);
    }

    @FunctionalInterface
    public static interface OperationContents {
        public InsnList generate(int var1, Consumer<InsnList> var2);
    }
}

