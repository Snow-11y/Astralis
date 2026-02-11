package org.spongepowered.asm.mixin.injection.invoke.arg;

public class ArgumentCountException
extends IllegalArgumentException {
    public ArgumentCountException(int received, int expected, String desc) {
        super("Invalid number of arguments for setAll, received " + received + " but expected " + expected + ": " + desc);
    }
}

