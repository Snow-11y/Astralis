package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessor;
import org.spongepowered.asm.util.perf.Profiler;

class MixinCoprocessors
extends ArrayList<MixinCoprocessor> {
    private final Profiler profiler = Profiler.getProfiler("mixin");

    MixinCoprocessors() {
    }

    MixinCoprocessor.ProcessResult process(String className, ClassNode classNode) {
        Profiler.Section timer = this.profiler.begin("coprocessor");
        MixinCoprocessor.ProcessResult result = MixinCoprocessor.ProcessResult.NONE;
        for (MixinCoprocessor coprocessor : this) {
            result = coprocessor.process(className, classNode).with(result);
        }
        timer.end();
        return result;
    }

    boolean postProcess(String className, ClassNode classNode) {
        Profiler.Section timer = this.profiler.begin("coprocessor");
        boolean transformed = false;
        for (MixinCoprocessor coprocessor : this) {
            transformed |= coprocessor.postProcess(className, classNode);
        }
        timer.end();
        return transformed;
    }
}

