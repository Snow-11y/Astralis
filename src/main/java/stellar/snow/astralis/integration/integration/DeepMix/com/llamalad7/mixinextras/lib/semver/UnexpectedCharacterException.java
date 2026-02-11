package com.llamalad7.mixinextras.lib.semver;

import com.llamalad7.mixinextras.lib.semver.ParseException;
import com.llamalad7.mixinextras.lib.semver.VersionParser;
import com.llamalad7.mixinextras.lib.semver.util.UnexpectedElementException;
import java.util.Arrays;

public class UnexpectedCharacterException
extends ParseException {
    private final Character unexpected;
    private final int position;
    private final VersionParser.CharType[] expected;

    UnexpectedCharacterException(UnexpectedElementException cause) {
        this((Character)cause.getUnexpectedElement(), cause.getPosition(), (VersionParser.CharType[])cause.getExpectedElementTypes());
    }

    UnexpectedCharacterException(Character unexpected, int position, VersionParser.CharType ... expected) {
        super(UnexpectedCharacterException.createMessage(unexpected, position, expected));
        this.unexpected = unexpected;
        this.position = position;
        this.expected = expected;
    }

    @Override
    public String toString() {
        return this.getMessage();
    }

    private static String createMessage(Character unexpected, int position, VersionParser.CharType ... expected) {
        String msg = String.format("Unexpected character %s(%s) at position %d", VersionParser.CharType.forCharacter(unexpected), unexpected, position);
        if (expected.length > 0) {
            msg = msg + String.format(", expecting %s", Arrays.toString(expected));
        }
        return msg;
    }
}

