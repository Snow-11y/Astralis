package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.IntStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Interval;

public interface CharStream
extends IntStream {
    public String getText(Interval var1);
}

