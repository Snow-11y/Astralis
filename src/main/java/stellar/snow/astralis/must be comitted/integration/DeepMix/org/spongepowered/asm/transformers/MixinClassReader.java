package org.spongepowered.asm.transformers;

import org.objectweb.asm.ClassReader;
import org.spongepowered.asm.util.asm.ASM;

public class MixinClassReader
extends ClassReader {
    public MixinClassReader(byte[] classFile, String name) {
        super(MixinClassReader.checkClassVersion(classFile, name));
    }

    private static byte[] checkClassVersion(byte[] classFile, String name) {
        short majorClassVersion = (short)((classFile[6] & 0xFF) << 8 | classFile[7] & 0xFF);
        if (majorClassVersion > ASM.getMaxSupportedClassVersionMajor()) {
            throw new IllegalArgumentException(String.format("Class file major version %d is not supported by active ASM (version %d.%d supports class version %d), reading %s", majorClassVersion, ASM.getApiVersionMajor(), ASM.getApiVersionMinor(), ASM.getMaxSupportedClassVersionMajor(), name));
        }
        return classFile;
    }
}

