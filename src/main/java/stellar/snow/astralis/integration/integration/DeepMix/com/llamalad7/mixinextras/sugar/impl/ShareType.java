package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefClassGenerator;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.ASMUtils;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ShareType {
    private final Type innerType;

    public ShareType(Type innerType) {
        this.innerType = innerType;
    }

    public Type getInnerType() {
        return this.innerType;
    }

    public Type getImplType() {
        return Type.getObjectType((String)LocalRefClassGenerator.getForType(this.innerType));
    }

    public InsnList initialize(int lvtIndex) {
        InsnList init = new InsnList();
        LocalRefUtils.generateNew(init, this.innerType);
        init.add((AbstractInsnNode)new VarInsnNode(58, lvtIndex));
        init.add((AbstractInsnNode)new VarInsnNode(25, lvtIndex));
        init.add((AbstractInsnNode)new InsnNode(ASMUtils.getDummyOpcodeForType(this.innerType)));
        LocalRefUtils.generateInitialization(init, this.innerType);
        return init;
    }

    public void addToLvt(Target target, int lvtIndex) {
        LabelNode start = new LabelNode();
        target.insns.insert((AbstractInsnNode)start);
        LabelNode end = new LabelNode();
        target.insns.add((AbstractInsnNode)end);
        Type implType = Type.getObjectType((String)LocalRefClassGenerator.getForType(this.innerType));
        target.addLocalVariable(lvtIndex, "sharedRef" + lvtIndex, implType.getDescriptor());
        List lvt = target.method.localVariables;
        LocalVariableNode newVar = (LocalVariableNode)lvt.get(lvt.size() - 1);
        newVar.start = start;
        newVar.end = end;
    }
}

