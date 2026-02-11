package org.spongepowered.asm.mixin.transformer;

import java.lang.reflect.Modifier;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.mixin.transformer.MixinPreProcessorStandard;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidInterfaceMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

class MixinPreProcessorInterface
extends MixinPreProcessorStandard {
    MixinPreProcessorInterface(MixinInfo mixin, MixinInfo.MixinClassNode classNode) {
        super(mixin, classNode);
    }

    @Override
    protected void prepareMethod(MixinInfo.MixinMethodNode mixinMethod, ClassInfo.Method method) {
        AnnotationNode injectorAnnotation;
        boolean isPublic = Bytecode.hasFlag(mixinMethod, 1);
        MixinEnvironment.Feature injectorsInInterfaceMixins = MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS;
        MixinEnvironment.CompatibilityLevel currentLevel = MixinEnvironment.getCompatibilityLevel();
        MixinEnvironment.CompatibilityLevel requiredLevelSynthetic = MixinEnvironment.CompatibilityLevel.requiredFor(2);
        if (!isPublic && mixinMethod.isSynthetic() && mixinMethod.isSynthetic()) {
            if (currentLevel.isLessThan(requiredLevelSynthetic)) {
                throw new InvalidInterfaceMixinException((IMixinInfo)this.mixin, String.format("Interface mixin contains a synthetic private method but compatibility level %s is required! Found %s in %s", new Object[]{requiredLevelSynthetic, method, this.mixin}));
            }
            return;
        }
        if (!isPublic) {
            if ("<clinit>".equals(mixinMethod.name) && "()V".equals(mixinMethod.desc)) {
                return;
            }
            MixinEnvironment.CompatibilityLevel requiredLevelPrivate = MixinEnvironment.CompatibilityLevel.requiredFor(4);
            if (currentLevel.isLessThan(requiredLevelPrivate)) {
                throw new InvalidInterfaceMixinException((IMixinInfo)this.mixin, String.format("Interface mixin contains a private method but compatibility level %s is required! Found %s in %s", new Object[]{requiredLevelPrivate, method, this.mixin}));
            }
        }
        if ((injectorAnnotation = InjectionInfo.getInjectorAnnotation(this.mixin, mixinMethod)) == null) {
            super.prepareMethod(mixinMethod, method);
            return;
        }
        if (injectorsInInterfaceMixins.isAvailable() && !injectorsInInterfaceMixins.isEnabled()) {
            throw new InvalidInterfaceMixinException((IMixinInfo)this.mixin, String.format("Interface mixin contains an injector but Feature.INJECTORS_IN_INTERFACE_MIXINS is disabled! Found %s in %s", method, this.mixin));
        }
        MixinEnvironment.CompatibilityLevel classLevel = MixinEnvironment.CompatibilityLevel.forClassVersion(this.mixin.getClassVersion());
        if (isPublic && !classLevel.supports(4) && classLevel.supports(2)) {
            Bytecode.setVisibility((MethodNode)mixinMethod, Bytecode.Visibility.PRIVATE);
            mixinMethod.access |= 0x1000;
        }
    }

    @Override
    protected boolean validateField(MixinTargetContext context, FieldNode field, AnnotationNode shadow) {
        if (!(Bytecode.isStatic(field) && Bytecode.hasFlag(field, 1) && Bytecode.hasFlag(field, 16))) {
            throw new InvalidInterfaceMixinException((IMixinInfo)this.mixin, String.format("Interface mixin contains an illegal field! Found %s %s in %s", Modifier.toString(field.access), field.name, this.mixin));
        }
        if (shadow == null) {
            throw new InvalidInterfaceMixinException((IMixinInfo)this.mixin, String.format("Interface mixin %s contains a non-shadow field: %s", this.mixin, field.name));
        }
        if (Annotations.getVisible(field, Mutable.class) != null) {
            throw new InvalidInterfaceMixinException((IMixinInfo)this.mixin, String.format("@Shadow field %s.%s is marked as mutable. This is not allowed.", this.mixin, field.name));
        }
        String prefix = (String)Annotations.getValue(shadow, "prefix", Shadow.class);
        if (field.name.startsWith(prefix)) {
            throw new InvalidMixinException((IMixinContext)context, String.format("@Shadow field %s.%s has a shadow prefix. This is not allowed.", context, field.name));
        }
        if ("super$".equals(field.name)) {
            throw new InvalidInterfaceMixinException((IMixinInfo)this.mixin, String.format("Interface mixin %s contains an imaginary super. This is not allowed", this.mixin));
        }
        return true;
    }
}

