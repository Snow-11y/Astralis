package org.spongepowered.asm.mixin.transformer.struct;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ListIterator;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

public class InsnRange {
    public final int start;
    public final int end;
    public final int marker;

    public InsnRange(int start, int end, int marker) {
        this.start = start;
        this.end = end;
        this.marker = marker;
    }

    public boolean isValid() {
        return this.start != 0 && this.end != 0 && this.end >= this.start;
    }

    public boolean contains(int value) {
        return value >= this.start && value <= this.end;
    }

    public boolean excludes(int value) {
        return value < this.start || value > this.end;
    }

    public String toString() {
        return String.format("Range[%d-%d,%d,valid=%s)", this.start, this.end, this.marker, this.isValid());
    }

    public Deque<AbstractInsnNode> apply(InsnList insns, boolean inclusive) {
        ArrayDeque<AbstractInsnNode> filtered = new ArrayDeque<AbstractInsnNode>();
        int line = 0;
        boolean gatherNodes = false;
        int trimAtOpcode = -1;
        LabelNode optionalInsn = null;
        ListIterator iter = insns.iterator(this.marker);
        while (iter.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)iter.next();
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
                AbstractInsnNode next = insns.get(insns.indexOf(insn) + 1);
                if (line == this.end && next.getOpcode() != 177) {
                    gatherNodes = !inclusive;
                    trimAtOpcode = 177;
                    continue;
                }
                gatherNodes = inclusive ? this.contains(line) : this.excludes(line);
                trimAtOpcode = -1;
                continue;
            }
            if (!gatherNodes) continue;
            if (optionalInsn != null) {
                filtered.add((AbstractInsnNode)optionalInsn);
                optionalInsn = null;
            }
            if (insn instanceof LabelNode) {
                optionalInsn = (LabelNode)insn;
                continue;
            }
            int opcode = insn.getOpcode();
            if (opcode == trimAtOpcode) {
                trimAtOpcode = -1;
                continue;
            }
            filtered.add(insn);
        }
        return filtered;
    }
}

