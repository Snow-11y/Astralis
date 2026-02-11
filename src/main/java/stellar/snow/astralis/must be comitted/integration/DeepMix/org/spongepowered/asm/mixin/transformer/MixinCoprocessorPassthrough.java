package org.spongepowered.asm.mixin.transformer;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessor;
import org.spongepowered.asm.mixin.transformer.MixinInfo;

class MixinCoprocessorPassthrough
extends MixinCoprocessor {
    private final Set<String> loadable = new HashSet<String>();

    MixinCoprocessorPassthrough() {
    }

    @Override
    public void onPrepare(MixinInfo mixin) {
        if (mixin.isLoadable()) {
            this.registerLoadable(mixin.getClassName());
        }
    }

    void registerLoadable(String className) {
        this.loadable.add(className);
    }

    @Override
    MixinCoprocessor.ProcessResult process(String className, ClassNode classNode) {
        return this.loadable.contains(className) ? MixinCoprocessor.ProcessResult.PASSTHROUGH_NONE : MixinCoprocessor.ProcessResult.NONE;
    }
}

