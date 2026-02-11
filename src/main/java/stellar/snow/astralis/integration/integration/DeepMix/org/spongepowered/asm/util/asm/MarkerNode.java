package org.spongepowered.asm.util.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.LabelNode;

public class MarkerNode
extends LabelNode {
    public static final int INITIALISER_TAIL = 1;
    public static final int BODY_START = 2;
    public final int type;

    public MarkerNode(int type) {
        super(null);
        this.type = type;
    }

    public void accept(MethodVisitor methodVisitor) {
    }
}

