package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.gen.AccessorGenerator;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.util.Bytecode;

public class AccessorGeneratorMethodProxy
extends AccessorGenerator {
    protected final MethodNode targetMethod;
    protected final Type[] argTypes;
    protected final Type returnType;

    public AccessorGeneratorMethodProxy(AccessorInfo info) {
        super(info, Bytecode.isStatic(info.getTargetMethod()));
        this.targetMethod = info.getTargetMethod();
        this.argTypes = info.getArgTypes();
        this.returnType = info.getReturnType();
        this.checkModifiers();
    }

    protected AccessorGeneratorMethodProxy(AccessorInfo info, boolean isStatic) {
        super(info, isStatic);
        this.targetMethod = info.getTargetMethod();
        this.argTypes = info.getArgTypes();
        this.returnType = info.getReturnType();
    }

    @Override
    public MethodNode generate() {
        int size = Bytecode.getArgsSize(this.argTypes) + this.returnType.getSize() + (this.targetIsStatic ? 0 : 1);
        MethodNode method = this.createMethod(size, size);
        if (!this.targetIsStatic) {
            method.instructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
        }
        Bytecode.loadArgs(this.argTypes, method.instructions, this.info.isStatic ? 0 : 1);
        boolean isInterface = Bytecode.hasFlag(this.info.getClassNode(), 512);
        boolean isPrivate = Bytecode.hasFlag(this.targetMethod, 2);
        int opcode = this.targetIsStatic ? 184 : (isInterface ? 185 : (isPrivate ? 183 : 182));
        method.instructions.add((AbstractInsnNode)new MethodInsnNode(opcode, this.info.getClassNode().name, this.targetMethod.name, this.targetMethod.desc, isInterface));
        method.instructions.add((AbstractInsnNode)new InsnNode(this.returnType.getOpcode(172)));
        return method;
    }
}

