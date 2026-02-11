package org.spongepowered.asm.mixin.transformer;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.MixinConfig;
import org.spongepowered.asm.mixin.transformer.MixinInfo;

abstract class MixinCoprocessor
implements MixinConfig.IListener {
    MixinCoprocessor() {
    }

    @Override
    public void onPrepare(MixinInfo mixin) {
    }

    @Override
    public void onInit(MixinInfo mixin) {
    }

    ProcessResult process(String className, ClassNode classNode) {
        return ProcessResult.NONE;
    }

    boolean postProcess(String className, ClassNode classNode) {
        return false;
    }

    static enum ProcessResult {
        NONE(false, false),
        TRANSFORMED(false, true),
        PASSTHROUGH_NONE(true, false),
        PASSTHROUGH_TRANSFORMED(true, true);

        private boolean passthrough;
        private boolean transformed;

        private ProcessResult(boolean passthrough, boolean transformed) {
            this.passthrough = passthrough;
            this.transformed = transformed;
        }

        boolean isPassthrough() {
            return this.passthrough;
        }

        boolean isTransformed() {
            return this.transformed;
        }

        ProcessResult with(ProcessResult other) {
            if (other == this) {
                return this;
            }
            return ProcessResult.of(this.passthrough || other.passthrough, this.transformed || other.transformed);
        }

        static ProcessResult of(boolean passthrough, boolean transformed) {
            if (passthrough) {
                return transformed ? PASSTHROUGH_TRANSFORMED : PASSTHROUGH_NONE;
            }
            return transformed ? TRANSFORMED : NONE;
        }
    }
}

