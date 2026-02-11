package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.MethodCallType;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.util.Bytecode;

public class CallTaggingPostProcessor
implements FlowPostProcessor {
    private final Type currentType;
    private final boolean isStatic;

    public CallTaggingPostProcessor(ClassNode classNode, MethodNode methodNode) {
        this.currentType = Type.getObjectType((String)classNode.name);
        this.isStatic = Bytecode.isStatic(methodNode);
    }

    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        MethodCallType type = this.getType(node);
        if (type == null) {
            return;
        }
        node.decorate("methodCallType", type);
        if (type == MethodCallType.SUPER) {
            sink.markAsSynthetic(node.getInput(0));
            node.removeParent(0);
        }
    }

    private MethodCallType getType(FlowValue node) {
        if (!(node.getInsn() instanceof MethodInsnNode)) {
            return null;
        }
        MethodInsnNode call = (MethodInsnNode)node.getInsn();
        switch (call.getOpcode()) {
            case 182: 
            case 185: {
                return MethodCallType.NORMAL;
            }
            case 184: {
                return MethodCallType.STATIC;
            }
            case 183: {
                if (call.name.equals("<init>")) {
                    return null;
                }
                if (call.owner.equals(this.currentType.getInternalName())) {
                    return MethodCallType.NORMAL;
                }
                if (!this.isLoadThis(node.getInput(0))) break;
                return MethodCallType.SUPER;
            }
        }
        return null;
    }

    private boolean isLoadThis(FlowValue node) {
        if (this.isStatic || node.isComplex() || node.getInsn().getOpcode() != 25) {
            return false;
        }
        VarInsnNode load = (VarInsnNode)node.getInsn();
        return load.var == 0;
    }
}

