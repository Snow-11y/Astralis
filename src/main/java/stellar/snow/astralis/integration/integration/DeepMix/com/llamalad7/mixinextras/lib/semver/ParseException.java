package com.llamalad7.mixinextras.lib.semver;

import com.llamalad7.mixinextras.lib.semver.UnexpectedCharacterException;

public class ParseException
extends RuntimeException {
    public ParseException() {
    }

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, UnexpectedCharacterException cause) {
        super(message);
        this.initCause(cause);
    }

    @Override
    public String toString() {
        Throwable cause = this.getCause();
        String msg = this.getMessage();
        if (msg != null) {
            msg = msg + (cause != null ? " (" + cause + ")" : "");
            return msg;
        }
        return cause != null ? cause.toString() : "";
    }
}

