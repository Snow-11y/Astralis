package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.expression.impl.flow.ComplexFlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.FlowContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

public class FlowValue
implements Value {
    private final Type type;
    private AbstractInsnNode insn;
    protected FlowValue[] parents;
    private final Set<Pair<FlowValue, Integer>> next = new HashSet<Pair<FlowValue, Integer>>(1);
    private boolean nextIsReady;
    private Map<String, Object> decorations = null;

    public FlowValue(Type type, AbstractInsnNode insn, FlowValue ... parents) {
        this.type = type;
        this.insn = insn;
        this.parents = parents;
    }

    public void addChild(FlowValue value, int index) {
        if (this.nextIsReady) {
            return;
        }
        this.next.add(Pair.of(value, index));
    }

    public void finish() {
        for (int i = 0; i < this.parents.length; ++i) {
            this.parents[i].addChild(this, i);
        }
    }

    public void onFinished() {
        this.nextIsReady = true;
    }

    private void markNextDirty() {
        this.nextIsReady = false;
        this.next.clear();
    }

    public int getSize() {
        return this.type.getSize();
    }

    public Type getType() {
        return this.type;
    }

    public AbstractInsnNode getInsn() {
        return this.insn;
    }

    public Collection<Pair<FlowValue, Integer>> getNext() {
        return this.next;
    }

    public FlowValue getInput(int index) {
        return this.parents[index];
    }

    public int inputCount() {
        return this.parents.length;
    }

    public void setInsn(AbstractInsnNode insn) {
        this.insn = insn;
    }

    public void setParents(FlowValue ... parents) {
        for (FlowValue parent : this.parents) {
            parent.markNextDirty();
        }
        this.parents = parents;
    }

    public void setParent(int index, FlowValue value) {
        this.parents[index].markNextDirty();
        this.parents[index] = value;
    }

    public void removeParent(int index) {
        this.setParents(ArrayUtils.remove(this.parents, index));
    }

    public FlowValue mergeWith(FlowValue other, FlowContext ctx) {
        if (this.equals(other)) {
            return this;
        }
        if (other instanceof ComplexFlowValue) {
            return other.mergeWith(this, ctx);
        }
        if (this.isTypeKnown() && other.isTypeKnown()) {
            return new DummyFlowValue(ExpressionASMUtils.getCommonSupertype(ctx, this.getType(), other.getType()));
        }
        return new ComplexFlowValue(this.getSize(), new HashSet<FlowValue>(Arrays.asList(this, other)), ctx);
    }

    public void mergeInputs(FlowValue[] newInputs, FlowContext ctx) {
        for (int i = 0; i < this.parents.length; ++i) {
            this.parents[i] = this.parents[i].mergeWith(newInputs[i], ctx);
        }
    }

    private boolean isTypeKnown() {
        return this.type != null;
    }

    public boolean isComplex() {
        return this.insn == null;
    }

    public <V> void decorate(String key, V value) {
        if (this.decorations == null) {
            this.decorations = new HashMap<String, Object>();
        }
        this.decorations.put(key, value);
    }

    public boolean hasDecoration(String key) {
        return this.decorations != null && this.decorations.get(key) != null;
    }

    public <V> V getDecoration(String key) {
        return (V)(this.decorations == null ? null : this.decorations.get(key));
    }

    public Map<String, Object> getDecorations() {
        return this.decorations == null ? Collections.emptyMap() : this.decorations;
    }

    public boolean typeMatches(Type desiredType) {
        if (ExpressionASMUtils.isIntLike(desiredType) && this.getType().equals((Object)ExpressionASMUtils.INTLIKE_TYPE)) {
            return true;
        }
        return this.getType().equals((Object)desiredType);
    }
}

