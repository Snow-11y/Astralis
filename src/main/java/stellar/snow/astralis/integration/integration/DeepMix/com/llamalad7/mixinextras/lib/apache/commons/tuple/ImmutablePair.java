package com.llamalad7.mixinextras.lib.apache.commons.tuple;

import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;

public final class ImmutablePair<L, R>
extends Pair<L, R> {
    public final L left;
    public final R right;

    public ImmutablePair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public L getLeft() {
        return this.left;
    }

    @Override
    public R getRight() {
        return this.right;
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }
}

