package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo;
import com.llamalad7.mixinextras.expression.impl.pool.MemberDefinition;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface SimpleMemberDefinition
extends MemberDefinition {
    public boolean matches(AbstractInsnNode var1);

    default public boolean matches(Handle handle) {
        return false;
    }

    @Override
    default public boolean matches(FlowValue node) {
        LMFInfo lmfInfo = (LMFInfo)node.getDecoration("lmfInfo");
        if (lmfInfo != null) {
            return this.matches(lmfInfo.impl);
        }
        return this.matches(node.getInsn());
    }
}

