package org.spongepowered.asm.mixin.injection.code;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.code.ISliceContext;
import org.spongepowered.asm.mixin.injection.code.InsnListEx;
import org.spongepowered.asm.mixin.injection.code.InsnListReadOnly;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointAnnotationContext;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidSliceException;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.include.com.google.common.base.Strings;

public final class MethodSlice {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private final ISliceContext owner;
    private final String id;
    private final InjectionPoint from;
    private final InjectionPoint to;
    private final String name;
    private int successCountFrom;
    private int successCountTo;

    private MethodSlice(ISliceContext owner, String id, InjectionPoint from, InjectionPoint to) {
        if (from == null && to == null) {
            throw new InvalidSliceException(owner, String.format("%s is redundant. No 'from' or 'to' value specified", this));
        }
        this.owner = owner;
        this.id = Strings.nullToEmpty(id);
        this.from = from;
        this.to = to;
        this.name = MethodSlice.getSliceName(id);
    }

    public String getId() {
        return this.id;
    }

    public InsnListReadOnly getSlice(Target target) {
        int end;
        int max = target.insns.size() - 1;
        int start = this.find(target, this.from, 0, 0, "from");
        if (start > (end = this.find(target, this.to, max, start, "to"))) {
            throw new InvalidSliceException(this.owner, String.format("%s is negative size. Range(%d -> %d)", this.describe(), start, end));
        }
        if (start < 0 || end < 0 || start > max || end > max) {
            throw new InjectionError("Unexpected critical error in " + this + ": out of bounds start=" + start + " end=" + end + " lim=" + max);
        }
        if (start == 0 && end == max) {
            return new InsnListEx(target);
        }
        return new InsnListSlice(target, start, end);
    }

    private int find(Target target, InjectionPoint injectionPoint, int defaultValue, int failValue, String argument) {
        if (injectionPoint == null) {
            return defaultValue;
        }
        String description = String.format("%s(%s)", this.name, argument);
        LinkedList<AbstractInsnNode> nodes = new LinkedList<AbstractInsnNode>();
        InsnListEx insns = new InsnListEx(target);
        boolean result = injectionPoint.find(target.getDesc(), insns, nodes);
        InjectionPoint.Specifier specifier = injectionPoint.getSpecifier(InjectionPoint.Specifier.FIRST);
        if (specifier == InjectionPoint.Specifier.ALL) {
            throw new InvalidSliceException(this.owner, String.format("ALL is not a valid specifier for slice %s", this.describe(description)));
        }
        if (nodes.size() != 1 && specifier == InjectionPoint.Specifier.ONE) {
            throw new InvalidSliceException(this.owner, String.format("%s requires 1 result but found %d", this.describe(description), nodes.size()));
        }
        if (!result) {
            return failValue;
        }
        if ("from".equals(argument)) {
            ++this.successCountFrom;
        } else {
            ++this.successCountTo;
        }
        return target.indexOf(specifier == InjectionPoint.Specifier.FIRST ? (AbstractInsnNode)nodes.getFirst() : (AbstractInsnNode)nodes.getLast());
    }

    public void postInject() {
        if (this.owner.getMixin().getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
            if (this.from != null && this.successCountFrom == 0) {
                logger.warn("{} did not match any instructions", this.describe(this.name + "(from)"));
            }
            if (this.to != null && this.successCountTo == 0) {
                logger.warn("{} did not match any instructions", this.describe(this.name + "(to)"));
            }
        }
    }

    public String toString() {
        return this.describe();
    }

    private String describe() {
        return this.describe(this.name);
    }

    private String describe(String description) {
        return MethodSlice.describeSlice(description, this.owner);
    }

    private static String describeSlice(String description, ISliceContext owner) {
        String annotation = Annotations.getSimpleName(owner.getAnnotationNode());
        MethodNode method = owner.getMethod();
        return String.format("%s->%s(%s)::%s%s", owner.getMixin(), annotation, description, method.name, method.desc);
    }

    private static String getSliceName(String id) {
        return String.format("@Slice[%s]", Strings.nullToEmpty(id));
    }

    public static MethodSlice parse(ISliceContext owner, Slice slice) {
        String id = slice.id();
        At from = slice.from();
        At to = slice.to();
        InjectionPoint fromPoint = from != null ? InjectionPoint.parse((IInjectionPointContext)owner, from) : null;
        InjectionPoint toPoint = to != null ? InjectionPoint.parse((IInjectionPointContext)owner, to) : null;
        return new MethodSlice(owner, id, fromPoint, toPoint);
    }

    public static MethodSlice parse(ISliceContext info, AnnotationNode node) {
        String id = (String)Annotations.getValue(node, "id");
        String coord = "slice";
        if (!Strings.isNullOrEmpty(id)) {
            coord = coord + "." + id;
        }
        InjectionPointAnnotationContext sliceContext = new InjectionPointAnnotationContext((IInjectionPointContext)info, node, coord);
        AnnotationNode from = (AnnotationNode)Annotations.getValue(node, "from");
        AnnotationNode to = (AnnotationNode)Annotations.getValue(node, "to");
        InjectionPoint fromPoint = from != null ? InjectionPoint.parse((IInjectionPointContext)new InjectionPointAnnotationContext((IInjectionPointContext)sliceContext, from, "from"), from) : null;
        InjectionPoint toPoint = to != null ? InjectionPoint.parse((IInjectionPointContext)new InjectionPointAnnotationContext((IInjectionPointContext)sliceContext, to, "to"), to) : null;
        return new MethodSlice(info, id, fromPoint, toPoint);
    }

    static final class InsnListSlice
    extends InsnListEx {
        private final int start;
        private final int end;

        protected InsnListSlice(Target target, int start, int end) {
            super(target);
            this.start = start;
            this.end = end;
        }

        @Override
        public ListIterator<AbstractInsnNode> iterator() {
            return this.iterator(0);
        }

        @Override
        public ListIterator<AbstractInsnNode> iterator(int index) {
            return new SliceIterator(super.iterator(this.start + index), this.start, this.end, this.start + index);
        }

        @Override
        public AbstractInsnNode[] toArray() {
            AbstractInsnNode[] all = super.toArray();
            AbstractInsnNode[] subset = new AbstractInsnNode[this.size()];
            System.arraycopy(all, this.start, subset, 0, subset.length);
            return subset;
        }

        @Override
        public int size() {
            return this.end - this.start + 1;
        }

        @Override
        public AbstractInsnNode getFirst() {
            return super.get(this.start);
        }

        @Override
        public AbstractInsnNode getLast() {
            return super.get(this.end);
        }

        @Override
        public AbstractInsnNode get(int index) {
            return super.get(this.start + index);
        }

        @Override
        public boolean contains(AbstractInsnNode insn) {
            if (insn == null) {
                return false;
            }
            for (AbstractInsnNode node : this.toArray()) {
                if (node != insn) continue;
                return true;
            }
            return false;
        }

        @Override
        public int indexOf(AbstractInsnNode insn) {
            int index = super.indexOf(insn);
            return index >= this.start && index <= this.end ? index - this.start : -1;
        }

        static class SliceIterator
        implements ListIterator<AbstractInsnNode> {
            private final ListIterator<AbstractInsnNode> iter;
            private int start;
            private int end;
            private int index;

            public SliceIterator(ListIterator<AbstractInsnNode> iter, int start, int end, int index) {
                this.iter = iter;
                this.start = start;
                this.end = end;
                this.index = index;
            }

            @Override
            public boolean hasNext() {
                return this.index <= this.end && this.iter.hasNext();
            }

            @Override
            public AbstractInsnNode next() {
                if (this.index > this.end) {
                    throw new NoSuchElementException();
                }
                ++this.index;
                return this.iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return this.index > this.start;
            }

            @Override
            public AbstractInsnNode previous() {
                if (this.index <= this.start) {
                    throw new NoSuchElementException();
                }
                --this.index;
                return this.iter.previous();
            }

            @Override
            public int nextIndex() {
                return this.index - this.start;
            }

            @Override
            public int previousIndex() {
                return this.index - this.start - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove insn from slice");
            }

            @Override
            public void set(AbstractInsnNode e) {
                throw new UnsupportedOperationException("Cannot set insn using slice");
            }

            @Override
            public void add(AbstractInsnNode e) {
                throw new UnsupportedOperationException("Cannot add insn using slice");
            }
        }
    }
}

