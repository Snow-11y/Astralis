package com.llamalad7.mixinextras.lib.semver.expr;

import com.llamalad7.mixinextras.lib.semver.Version;
import java.util.function.Predicate;

public interface Expression
extends Predicate<Version> {
    public boolean interpret(Version var1);

    @Override
    default public boolean test(Version version) {
        return this.interpret(version);
    }
}

