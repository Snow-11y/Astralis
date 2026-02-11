package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.struct.IChainedDecoration;

public class ArgOffsets
implements IChainedDecoration<ArgOffsets> {
    public static ArgOffsets DEFAULT = new Default();
    public static final String KEY = "argOffsets";
    private final int offset;
    private final int length;
    private ArgOffsets next;

    public ArgOffsets(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    public String toString() {
        return String.format("ArgOffsets[start=%d(%d),length=%d]", this.offset, this.getStartIndex(), this.length);
    }

    @Override
    public void replace(ArgOffsets old) {
        this.next = old;
    }

    public int getLength() {
        return this.length;
    }

    public boolean isEmpty() {
        return this.length == 0;
    }

    public int getStartIndex() {
        return this.getArgIndex(0);
    }

    public int getEndIndex() {
        return this.isEmpty() ? this.getStartIndex() : this.getArgIndex(this.length - 1);
    }

    public int getArgIndex(int index) {
        return this.getArgIndex(index, false);
    }

    public int getArgIndex(int index, boolean mustBeInWindow) {
        if (mustBeInWindow && index > this.length) {
            throw new IndexOutOfBoundsException("The specified arg index " + index + " is greater than the window size " + this.length);
        }
        int offsetIndex = index + this.offset;
        return this.next != null ? this.next.getArgIndex(offsetIndex) : offsetIndex;
    }

    public Type[] apply(Type[] args) {
        Type[] transformed = new Type[this.length];
        for (int i = 0; i < this.length; ++i) {
            int offset = this.getArgIndex(i);
            if (offset >= args.length) continue;
            transformed[i] = args[offset];
        }
        return transformed;
    }

    private static class Default
    extends ArgOffsets {
        public Default() {
            super(0, 255);
        }

        @Override
        public int getArgIndex(int index) {
            return index;
        }

        @Override
        public Type[] apply(Type[] args) {
            return args;
        }
    }
}

