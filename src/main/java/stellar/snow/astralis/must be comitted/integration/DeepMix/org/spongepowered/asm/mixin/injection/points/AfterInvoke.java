package org.spongepowered.asm.mixin.injection.points;

import java.util.Arrays;
import java.util.Collection;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.points.BeforeInvoke;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

@InjectionPoint.AtCode(value="INVOKE_ASSIGN")
public class AfterInvoke
extends BeforeInvoke {
    public static final int[] DEFAULT_SKIP = new int[]{89, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 192, 193};
    private int fuzz = 1;
    private int[] skip = null;

    public AfterInvoke(InjectionPointData data) {
        super(data);
        this.fuzz = Math.max(data.get("fuzz", this.fuzz), 1);
        this.skip = data.getOpcodeList("skip", DEFAULT_SKIP);
    }

    @Override
    protected boolean addInsn(InsnList insns, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
        MethodInsnNode methodNode = (MethodInsnNode)insn;
        if (Type.getReturnType((String)methodNode.desc) == Type.VOID_TYPE) {
            return false;
        }
        if (this.fuzz > 0) {
            int offset = insns.indexOf(insn);
            int maxOffset = Math.min(insns.size(), offset + this.fuzz + 1);
            for (int index = offset + 1; index < maxOffset; ++index) {
                AbstractInsnNode candidate = insns.get(index);
                if (candidate instanceof VarInsnNode && insn.getOpcode() >= 54) {
                    insn = candidate;
                    break;
                }
                if (this.skip != null && this.skip.length > 0 && Arrays.binarySearch(this.skip, candidate.getOpcode()) < 0) break;
            }
        }
        insn = InjectionPoint.nextNode(insns, insn);
        nodes.add(insn);
        return true;
    }
}

