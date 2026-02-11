package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.points.BeforeInvoke;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

@InjectionPoint.AtCode(value="INVOKE_STRING")
public class BeforeStringInvoke
extends BeforeInvoke {
    private final String ldcValue;
    private boolean foundLdc;

    public BeforeStringInvoke(InjectionPointData data) {
        super(data);
        this.ldcValue = data.get("ldc", (String)null);
        if (this.ldcValue == null) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " requires named argument \"ldc\" to specify the desired target");
        }
        if (!(this.target instanceof ITargetSelectorByName) || !"(Ljava/lang/String;)V".equals(((ITargetSelectorByName)this.target).getDesc())) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " requires target method with with signature " + "(Ljava/lang/String;)V");
        }
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        this.foundLdc = false;
        return super.find(desc, insns, nodes);
    }

    @Override
    protected void inspectInsn(String desc, InsnList insns, AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode) {
            LdcInsnNode node = (LdcInsnNode)insn;
            if (node.cst instanceof String && this.ldcValue.equals(node.cst)) {
                this.log("{}->{} > found a matching LDC with value {}", this.context, this.className, node.cst);
                this.foundLdc = true;
                return;
            }
        }
        this.foundLdc = false;
    }

    @Override
    protected boolean matchesOrdinal(int ordinal) {
        this.log("{}->{} > > found LDC \"{}\" = {}", this.context, this.className, this.ldcValue, this.foundLdc);
        return this.foundLdc && super.matchesOrdinal(ordinal);
    }
}

