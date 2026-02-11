package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

@InjectionPoint.AtCode(value="TAIL")
public class BeforeFinalReturn
extends InjectionPoint {
    private final IMixinContext context;

    public BeforeFinalReturn(InjectionPointData data) {
        super(data);
        this.context = data.getMixin();
    }

    @Override
    public boolean checkPriority(int targetPriority, int ownerPriority) {
        return true;
    }

    @Override
    public InjectionPoint.RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
        return InjectionPoint.RestrictTargetLevel.ALLOW_ALL;
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        AbstractInsnNode ret = null;
        int returnOpcode = Type.getReturnType((String)desc).getOpcode(172);
        for (AbstractInsnNode insn : insns) {
            if (!(insn instanceof InsnNode) || insn.getOpcode() != returnOpcode) continue;
            ret = insn;
        }
        if (ret == null) {
            throw new InvalidInjectionException(this.context, "TAIL could not locate a valid RETURN in the target method!");
        }
        nodes.add(ret);
        return true;
    }
}

