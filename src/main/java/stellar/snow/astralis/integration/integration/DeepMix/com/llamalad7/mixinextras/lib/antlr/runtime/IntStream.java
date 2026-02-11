package com.llamalad7.mixinextras.lib.antlr.runtime;

public interface IntStream {
    public void consume();

    public int LA(int var1);

    public int mark();

    public void release(int var1);

    public int index();

    public void seek(int var1);

    public int size();
}

