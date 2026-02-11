package org.spongepowered.asm.mixin.transformer;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessor;
import org.spongepowered.asm.mixin.transformer.MixinInfo;

class MixinCoprocessorSyntheticInner
extends MixinCoprocessor {
    private final Set<String> syntheticInnerClasses = new HashSet<String>();

    MixinCoprocessorSyntheticInner() {
    }

    @Override
    public void onInit(MixinInfo mixin) {
        for (String innerClass : mixin.getSyntheticInnerClasses()) {
            this.registerSyntheticInner(innerClass.replace('/', '.'));
        }
    }

    void registerSyntheticInner(String className) {
        this.syntheticInnerClasses.add(className);
    }

    @Override
    MixinCoprocessor.ProcessResult process(String className, ClassNode classNode) {
        if (!this.syntheticInnerClasses.contains(className)) {
            return MixinCoprocessor.ProcessResult.NONE;
        }
        classNode.access |= 1;
        for (FieldNode field : classNode.fields) {
            if ((field.access & 6) != 0) continue;
            field.access |= 1;
        }
        for (MethodNode method : classNode.methods) {
            if ((method.access & 6) != 0) continue;
            method.access |= 1;
        }
        return MixinCoprocessor.ProcessResult.PASSTHROUGH_TRANSFORMED;
    }
}

