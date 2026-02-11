package org.spongepowered.asm.util.asm;

import org.objectweb.asm.MethodVisitor;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;

public class MethodVisitorEx
extends MethodVisitor {
    public MethodVisitorEx(MethodVisitor mv) {
        super(ASM.API_VERSION, mv);
    }

    public void visitConstant(byte constant) {
        if (constant > -2 && constant < 6) {
            this.visitInsn(Bytecode.CONSTANTS_INT[constant + 1]);
            return;
        }
        this.visitIntInsn(16, constant);
    }
}

