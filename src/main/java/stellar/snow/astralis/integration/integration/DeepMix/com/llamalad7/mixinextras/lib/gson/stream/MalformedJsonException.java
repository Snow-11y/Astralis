package com.llamalad7.mixinextras.lib.gson.stream;

import java.io.IOException;

public final class MalformedJsonException
extends IOException {
    public MalformedJsonException(String msg) {
        super(msg);
    }
}

