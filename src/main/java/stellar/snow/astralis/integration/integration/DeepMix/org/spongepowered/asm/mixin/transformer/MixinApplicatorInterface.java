package org.spongepowered.asm.mixin.transformer;

import java.util.Map;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinApplicatorStandard;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.TargetClassContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidInterfaceMixinException;
import org.spongepowered.asm.util.Annotations;

class MixinApplicatorInterface
extends MixinApplicatorStandard {
    MixinApplicatorInterface(TargetClassContext context) {
        super(context);
    }

    @Override
    protected void applyInterfaces(MixinTargetContext mixin) {
        for (String interfaceName : mixin.getInterfaces()) {
            if (this.targetClass.name.equals(interfaceName) || this.targetClass.interfaces.contains(interfaceName)) continue;
            this.targetClass.interfaces.add(interfaceName);
            mixin.getTargetClassInfo().addInterface(interfaceName);
        }
    }

    @Override
    protected void mergeShadowFields(MixinTargetContext mixin) {
        for (Map.Entry<FieldNode, ClassInfo.Field> entry : mixin.getShadowFields()) {
            FieldNode shadow = entry.getKey();
            FieldNode target = this.findTargetField(shadow);
            if (target != null) {
                Annotations.merge(shadow, target);
                if (entry.getValue().isDecoratedMutable()) {
                    this.logger.error("Ignoring illegal @Mutable on {}:{} in {}", shadow.name, shadow.desc, mixin);
                }
                if (shadow.value == null) continue;
                this.logger.warn("@Shadow field {}:{} in {} has an inlinable value set, is this intended?", shadow.name, shadow.desc, mixin);
                continue;
            }
            this.logger.warn("Unable to find target for @Shadow {}:{} in {}", shadow.name, shadow.desc, mixin);
        }
    }

    @Override
    protected void mergeNewFields(MixinTargetContext mixin) {
    }

    @Override
    protected void applyNormalMethod(MixinTargetContext mixin, MethodNode mixinMethod) {
        if (!"<clinit>".equals(mixinMethod.name)) {
            super.applyNormalMethod(mixin, mixinMethod);
        }
    }

    @Override
    protected void applyInitialisers(MixinTargetContext mixin) {
    }

    @Override
    protected void prepareInjections(MixinTargetContext mixin) {
        if (MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS.isEnabled()) {
            try {
                super.prepareInjections(mixin);
            }
            catch (InvalidInjectionException ex) {
                String description = ex.getContext() != null ? ex.getContext().toString() : "Injection";
                throw new InvalidInterfaceMixinException((IMixinContext)mixin, description + " is not supported in interface mixin", (Throwable)ex);
            }
            return;
        }
        InjectionInfo injectInfo = mixin.getFirstInjectionInfo();
        if (injectInfo != null) {
            throw new InvalidInterfaceMixinException((IMixinContext)mixin, injectInfo + " is not supported on interface mixin method " + injectInfo.getMethodName());
        }
    }

    @Override
    protected void applyPreInjections(MixinTargetContext mixin) {
        if (MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS.isEnabled()) {
            super.applyPreInjections(mixin);
            return;
        }
    }

    @Override
    protected void applyInjections(MixinTargetContext mixin, int injectorOrder) {
        if (MixinEnvironment.Feature.INJECTORS_IN_INTERFACE_MIXINS.isEnabled()) {
            super.applyInjections(mixin, injectorOrder);
            return;
        }
    }
}

