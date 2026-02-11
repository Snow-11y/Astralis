package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class LMFPostProcessor
implements FlowPostProcessor {
    private final Type currentType;

    public LMFPostProcessor(ClassNode classNode) {
        this.currentType = Type.getObjectType((String)classNode.name);
    }

    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        if (node.getInsn().getOpcode() != 186) {
            return;
        }
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode)node.getInsn();
        if (!indy.bsm.equals((Object)ExpressionASMUtils.LMF_HANDLE) && !indy.bsm.equals((Object)ExpressionASMUtils.ALT_LMF_HANDLE)) {
            return;
        }
        Handle impl = (Handle)indy.bsmArgs[1];
        LMFInfo.Type type = this.getType(node, impl);
        if (type == null) {
            return;
        }
        node.decorate("lmfInfo", new LMFInfo(impl, type));
        if (type == LMFInfo.Type.BOUND_METHOD) {
            this.transformReceiver(node, sink);
        }
    }

    private LMFInfo.Type getType(FlowValue node, Handle impl) {
        boolean bound = node.inputCount() != 0;
        switch (impl.getTag()) {
            case 8: {
                return bound ? null : LMFInfo.Type.INSTANTIATION;
            }
            case 7: {
                if (!impl.getOwner().equals(this.currentType.getInternalName())) {
                    return null;
                }
            }
            case 5: 
            case 9: {
                return bound ? LMFInfo.Type.BOUND_METHOD : LMFInfo.Type.FREE_METHOD;
            }
            case 6: {
                return LMFInfo.Type.FREE_METHOD;
            }
        }
        return null;
    }

    private void transformReceiver(FlowValue indy, FlowPostProcessor.OutputSink sink) {
        FlowValue receiver = indy.getInput(0);
        for (Pair<FlowValue, Integer> next : receiver.getNext()) {
            MethodInsnNode call;
            FlowValue child = next.getLeft();
            if (child == indy || next.getRight() != 0 || child.inputCount() != 1 || !child.getNext().isEmpty() || !(child.getInsn() instanceof MethodInsnNode) || !this.isGetClass(call = (MethodInsnNode)child.getInsn()) && !this.isRequireNonNull(call)) continue;
            sink.markAsSynthetic(child);
        }
    }

    private boolean isGetClass(MethodInsnNode call) {
        return call.getOpcode() == 182 && call.owner.equals("java/lang/Object") && call.name.equals("getClass") && call.desc.equals("()Ljava/lang/Class;");
    }

    private boolean isRequireNonNull(MethodInsnNode call) {
        return call.getOpcode() == 184 && call.owner.equals("java/util/Objects") && call.name.equals("requireNonNull") && call.desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;");
    }
}

