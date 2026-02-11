package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.gen.AccessorGeneratorMethodProxy;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;

public class AccessorGeneratorObjectFactory
extends AccessorGeneratorMethodProxy {
    public AccessorGeneratorObjectFactory(AccessorInfo info) {
        super(info, true);
        if (!info.isStatic()) {
            throw new InvalidInjectionException(info.getMixin(), String.format("%s is invalid. Factory method must be static.", this.info));
        }
    }

    @Override
    public MethodNode generate() {
        int returnSize = this.returnType.getSize();
        int size = Bytecode.getArgsSize(this.argTypes) + returnSize * 2;
        MethodNode method = this.createMethod(size, size);
        String className = this.info.getTargetClassNode().name;
        method.instructions.add((AbstractInsnNode)new TypeInsnNode(187, className));
        method.instructions.add((AbstractInsnNode)new InsnNode(returnSize == 1 ? 89 : 92));
        Bytecode.loadArgs(this.argTypes, method.instructions, 0);
        method.instructions.add((AbstractInsnNode)new MethodInsnNode(183, className, "<init>", this.targetMethod.desc, false));
        method.instructions.add((AbstractInsnNode)new InsnNode(176));
        return method;
    }
}

