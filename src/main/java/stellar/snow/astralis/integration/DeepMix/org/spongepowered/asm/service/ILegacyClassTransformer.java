package org.spongepowered.asm.service;

import org.spongepowered.asm.service.ITransformer;

public interface ILegacyClassTransformer
extends ITransformer {
    public byte[] transformClassBytes(String var1, String var2, byte[] var3);
}

