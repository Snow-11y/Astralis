package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.BaseErrorListener;
import com.llamalad7.mixinextras.lib.antlr.runtime.RecognitionException;
import com.llamalad7.mixinextras.lib.antlr.runtime.Recognizer;

public class ConsoleErrorListener
extends BaseErrorListener {
    public static final ConsoleErrorListener INSTANCE = new ConsoleErrorListener();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        System.err.println("line " + line + ":" + charPositionInLine + " " + msg);
    }
}

