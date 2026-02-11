package org.spongepowered.asm.mixin.transformer.ext.extensions;

import java.util.Iterator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.util.Locals;

public class ExtensionLVTCleaner
implements IExtension {
    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.localVariables == null) continue;
            Iterator it = methodNode.localVariables.iterator();
            while (it.hasNext()) {
                if (!(it.next() instanceof Locals.SyntheticLocalVariableNode)) continue;
                it.remove();
            }
        }
    }
}

