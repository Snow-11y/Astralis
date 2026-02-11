package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.gen.AccessorGeneratorField;
import org.spongepowered.asm.mixin.gen.AccessorInfo;

public class AccessorGeneratorFieldGetter
extends AccessorGeneratorField {
    public AccessorGeneratorFieldGetter(AccessorInfo info) {
        super(info);
    }

    @Override
    public MethodNode generate() {
        MethodNode method = this.createMethod(this.targetType.getSize(), this.targetType.getSize());
        if (!this.targetIsStatic) {
            method.instructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
        }
        int opcode = this.targetIsStatic ? 178 : 180;
        method.instructions.add((AbstractInsnNode)new FieldInsnNode(opcode, this.info.getTargetClassNode().name, this.targetField.name, this.targetField.desc));
        method.instructions.add((AbstractInsnNode)new InsnNode(this.targetType.getOpcode(172)));
        return method;
    }
}

