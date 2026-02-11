package org.spongepowered.asm.util;

import org.objectweb.asm.Handle;

public final class Handles {
    private static final int[] H_OPCODES = new int[]{0, 180, 178, 181, 179, 182, 184, 183, 183, 185};

    private Handles() {
    }

    public static boolean isField(Handle handle) {
        switch (handle.getTag()) {
            case 5: 
            case 6: 
            case 7: 
            case 8: 
            case 9: {
                return false;
            }
            case 1: 
            case 2: 
            case 3: 
            case 4: {
                return true;
            }
        }
        throw new IllegalArgumentException("Invalid tag " + handle.getTag() + " for method handle " + handle + ".");
    }

    public static int opcodeFromTag(int tag) {
        return tag >= 0 && tag < H_OPCODES.length ? H_OPCODES[tag] : 0;
    }

    public static int tagFromOpcode(int opcode) {
        for (int tag = 1; tag < H_OPCODES.length; ++tag) {
            if (H_OPCODES[tag] != opcode) continue;
            return tag;
        }
        return 0;
    }
}

