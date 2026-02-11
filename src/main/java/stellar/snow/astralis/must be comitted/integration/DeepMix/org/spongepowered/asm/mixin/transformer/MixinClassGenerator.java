package org.spongepowered.asm.mixin.transformer;

import java.util.Locale;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.perf.Profiler;

public class MixinClassGenerator {
    static final ILogger logger = MixinService.getService().getLogger("mixin");
    private final Extensions extensions;
    private final Profiler profiler;
    private final IMixinAuditTrail auditTrail;

    MixinClassGenerator(MixinEnvironment environment, Extensions extensions) {
        this.extensions = extensions;
        this.profiler = Profiler.getProfiler("generator");
        this.auditTrail = MixinService.getService().getAuditTrail();
    }

    synchronized boolean generateClass(MixinEnvironment environment, String name, ClassNode classNode) {
        if (name == null) {
            logger.warn("MixinClassGenerator tried to generate a class with no name!", new Object[0]);
            return false;
        }
        for (IClassGenerator generator : this.extensions.getGenerators()) {
            Profiler.Section genTimer = this.profiler.begin("generator", generator.getClass().getSimpleName().toLowerCase(Locale.ROOT));
            boolean success = generator.generate(name, classNode);
            genTimer.end();
            if (!success) continue;
            if (this.auditTrail != null) {
                this.auditTrail.onGenerate(name, generator.getName());
            }
            this.extensions.export(environment, name.replace('.', '/'), false, classNode);
            return true;
        }
        return false;
    }
}

