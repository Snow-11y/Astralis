package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

@InjectionPoint.AtCode(value="HEAD")
public class MethodHead
extends InjectionPoint {
    public MethodHead(InjectionPointData data) {
        super(data);
    }

    @Override
    public boolean checkPriority(int targetPriority, int ownerPriority) {
        return true;
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        nodes.add(insns.getFirst());
        return true;
    }
}

