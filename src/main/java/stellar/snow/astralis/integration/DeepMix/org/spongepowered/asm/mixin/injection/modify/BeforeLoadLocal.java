package org.spongepowered.asm.mixin.injection.modify;

import java.util.Collection;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.modify.InvalidImplicitDiscriminatorException;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.Target;

@InjectionPoint.AtCode(value="LOAD")
public class BeforeLoadLocal
extends ModifyVariableInjector.LocalVariableInjectionPoint {
    protected final Type returnType;
    protected final LocalVariableDiscriminator discriminator;
    protected final int opcode;
    protected final int ordinal;
    private boolean opcodeAfter;

    protected BeforeLoadLocal(InjectionPointData data) {
        this(data, 21, false);
    }

    protected BeforeLoadLocal(InjectionPointData data, int opcode, boolean opcodeAfter) {
        super(data);
        this.returnType = data.getMethodReturnType();
        this.discriminator = data.getLocalVariableDiscriminator();
        this.opcode = data.getOpcode(this.returnType.getOpcode(opcode));
        this.ordinal = data.getOrdinal();
        this.opcodeAfter = opcodeAfter;
    }

    @Override
    boolean find(InjectionInfo info, InsnList insns, Collection<AbstractInsnNode> nodes, Target target) {
        SearchState state = new SearchState();
        for (AbstractInsnNode insn : insns) {
            if (state.isPendingCheck()) {
                state.check(info, target, nodes, insn);
                continue;
            }
            if (!(insn instanceof VarInsnNode) || insn.getOpcode() != this.opcode || this.ordinal != -1 && state.success()) continue;
            state.register((VarInsnNode)insn);
            if (this.opcodeAfter) {
                state.setPendingCheck();
                continue;
            }
            state.check(info, target, nodes, insn);
        }
        return state.success();
    }

    @Override
    protected void addMessage(String format, Object ... args) {
        super.addMessage(format, args);
    }

    @Override
    public String toString() {
        return String.format("@At(\"%s\" %s)", this.getAtCode(), this.discriminator.toString());
    }

    public String toString(LocalVariableDiscriminator.Context context) {
        return String.format("@At(\"%s\" %s)", this.getAtCode(), this.discriminator.toString(context));
    }

    class SearchState {
        private final boolean print;
        private int currentOrdinal = 0;
        private boolean pendingCheck = false;
        private boolean found = false;
        private VarInsnNode varNode;

        SearchState() {
            this.print = BeforeLoadLocal.this.discriminator.printLVT();
        }

        boolean success() {
            return this.found;
        }

        boolean isPendingCheck() {
            return this.pendingCheck;
        }

        void setPendingCheck() {
            this.pendingCheck = true;
        }

        void register(VarInsnNode node) {
            this.varNode = node;
        }

        void check(InjectionInfo info, Target target, Collection<AbstractInsnNode> nodes, AbstractInsnNode insn) {
            LocalVariableDiscriminator.Context context = new LocalVariableDiscriminator.Context(info, BeforeLoadLocal.this.returnType, BeforeLoadLocal.this.discriminator.isArgsOnly(), target, insn);
            int local = -2;
            try {
                local = BeforeLoadLocal.this.discriminator.findLocal(context);
            }
            catch (InvalidImplicitDiscriminatorException ex) {
                BeforeLoadLocal.this.addMessage("%s has invalid IMPLICIT discriminator for opcode %d in %s: %s", BeforeLoadLocal.this.toString(context), target.indexOf(insn), target, ex.getMessage());
            }
            this.pendingCheck = false;
            if (!(local == this.varNode.var || local <= -2 && this.print)) {
                this.varNode = null;
                return;
            }
            if (BeforeLoadLocal.this.ordinal == -1 || BeforeLoadLocal.this.ordinal == this.currentOrdinal) {
                nodes.add(insn);
                this.found = true;
            }
            ++this.currentOrdinal;
            this.varNode = null;
        }
    }
}

