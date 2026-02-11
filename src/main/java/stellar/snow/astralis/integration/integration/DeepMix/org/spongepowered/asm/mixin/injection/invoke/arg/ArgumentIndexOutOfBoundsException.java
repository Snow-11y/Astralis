package org.spongepowered.asm.mixin.injection.invoke.arg;

public class ArgumentIndexOutOfBoundsException
extends IndexOutOfBoundsException {
    public ArgumentIndexOutOfBoundsException(int index) {
        super("Argument index is out of bounds: " + index);
    }
}

