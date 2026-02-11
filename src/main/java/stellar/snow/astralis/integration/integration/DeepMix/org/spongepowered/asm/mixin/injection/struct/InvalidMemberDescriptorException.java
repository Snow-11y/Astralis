package org.spongepowered.asm.mixin.injection.struct;

import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;

public class InvalidMemberDescriptorException
extends InvalidSelectorException {
    private final String input;

    public InvalidMemberDescriptorException(String input, String message) {
        super(message);
        this.input = input;
    }

    public InvalidMemberDescriptorException(String input, Throwable cause) {
        super(cause);
        this.input = input;
    }

    public InvalidMemberDescriptorException(String input, String message, Throwable cause) {
        super(message, cause);
        this.input = input;
    }

    public String getInput() {
        return this.input;
    }
}

