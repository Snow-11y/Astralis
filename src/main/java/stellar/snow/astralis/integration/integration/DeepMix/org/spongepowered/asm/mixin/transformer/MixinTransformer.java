package org.spongepowered.asm.mixin.transformer;

import java.lang.reflect.Constructor;
import java.util.List;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinProcessor;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.DefaultExtensions;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.mixin.transformer.MixinClassGenerator;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessorNestHost;
import org.spongepowered.asm.mixin.transformer.MixinProcessor;
import org.spongepowered.asm.mixin.transformer.SyntheticClassRegistry;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.mixin.transformer.ext.IHotSwap;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.asm.ASM;

public final class MixinTransformer
extends TreeTransformer
implements IMixinTransformer {
    private final SyntheticClassRegistry syntheticClassRegistry;
    private final Extensions extensions;
    private final IHotSwap hotSwapper;
    private final MixinCoprocessorNestHost nestHostCoprocessor;
    private final MixinProcessor processor;
    private final MixinClassGenerator generator;

    MixinTransformer() {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        Object globalMixinTransformer = environment.getActiveTransformer();
        if (globalMixinTransformer instanceof IMixinTransformer) {
            throw new MixinException("Terminating MixinTransformer instance " + this);
        }
        environment.setActiveTransformer(this);
        this.syntheticClassRegistry = new SyntheticClassRegistry();
        this.extensions = new Extensions(this.syntheticClassRegistry);
        this.hotSwapper = this.initHotSwapper(environment);
        this.nestHostCoprocessor = new MixinCoprocessorNestHost();
        this.processor = new MixinProcessor(environment, this.extensions, this.hotSwapper, this.nestHostCoprocessor);
        this.generator = new MixinClassGenerator(environment, this.extensions);
        DefaultExtensions.create(environment, this.extensions, this.syntheticClassRegistry, this.nestHostCoprocessor);
    }

    private IHotSwap initHotSwapper(MixinEnvironment environment) {
        if (!environment.getOption(MixinEnvironment.Option.HOT_SWAP)) {
            return null;
        }
        try {
            MixinProcessor.logger.info("Attempting to load Hot-Swap agent", new Object[0]);
            Class<?> clazz = Class.forName("org.spongepowered.tools.agent.MixinAgent");
            Constructor<?> ctor = clazz.getDeclaredConstructor(IMixinTransformer.class);
            return (IHotSwap)ctor.newInstance(this);
        }
        catch (Throwable th) {
            MixinProcessor.logger.info("Hot-swap agent could not be loaded, hot swapping of mixins won't work. {}: {}", th.getClass().getSimpleName(), th.getMessage());
            return null;
        }
    }

    @Override
    public IExtensionRegistry getExtensions() {
        return this.extensions;
    }

    @Override
    public IMixinProcessor getProcessor() {
        return this.processor;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public void audit(MixinEnvironment environment) {
        this.processor.audit(environment);
    }

    @Override
    public List<String> reload(String mixinClass, ClassNode classNode) {
        return this.processor.reload(mixinClass, classNode);
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (transformedName == null) {
            return basicClass;
        }
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        if (basicClass == null) {
            return this.generateClass(environment, transformedName);
        }
        return this.transformClass(environment, transformedName, basicClass);
    }

    @Override
    public boolean computeFramesForClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return false;
    }

    @Override
    public byte[] transformClass(MixinEnvironment environment, String name, byte[] classBytes) {
        ClassNode classNode = this.readClass(name, classBytes);
        if (this.processor.applyMixins(environment, name, classNode)) {
            return this.writeClass(classNode);
        }
        return classBytes;
    }

    @Override
    public boolean transformClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return this.processor.applyMixins(environment, name, classNode);
    }

    @Override
    public byte[] generateClass(MixinEnvironment environment, String name) {
        ClassNode classNode = MixinTransformer.createEmptyClass(name);
        if (this.generator.generateClass(environment, name, classNode)) {
            return this.writeClass(classNode);
        }
        return null;
    }

    @Override
    public boolean generateClass(MixinEnvironment environment, String name, ClassNode classNode) {
        return this.generator.generateClass(environment, name, classNode);
    }

    private static ClassNode createEmptyClass(String name) {
        ClassNode classNode = new ClassNode(ASM.API_VERSION);
        classNode.name = name.replace('.', '/');
        classNode.version = MixinEnvironment.getCompatibilityLevel().getClassVersion();
        classNode.superName = "java/lang/Object";
        return classNode;
    }

    static class Factory
    implements IMixinTransformerFactory {
        Factory() {
        }

        @Override
        public IMixinTransformer createTransformer() throws MixinInitialisationError {
            return new MixinTransformer();
        }
    }
}

