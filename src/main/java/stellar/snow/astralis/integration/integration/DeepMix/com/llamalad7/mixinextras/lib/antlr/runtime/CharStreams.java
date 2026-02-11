package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.CodePointBuffer;
import com.llamalad7.mixinextras.lib.antlr.runtime.CodePointCharStream;
import java.nio.CharBuffer;

public final class CharStreams {
    public static CodePointCharStream fromString(String s) {
        return CharStreams.fromString(s, "<unknown>");
    }

    public static CodePointCharStream fromString(String s, String sourceName) {
        CodePointBuffer.Builder codePointBufferBuilder = CodePointBuffer.builder(s.length());
        CharBuffer cb = CharBuffer.allocate(s.length());
        cb.put(s);
        cb.flip();
        codePointBufferBuilder.append(cb);
        return CodePointCharStream.fromBuffer(codePointBufferBuilder.build(), sourceName);
    }
}

