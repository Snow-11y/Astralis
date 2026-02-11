package org.spongepowered.tools.obfuscation.validation;

import java.util.Collection;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.MixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

public class TargetValidator
extends MixinValidator {
    public TargetValidator(IMixinAnnotationProcessor ap) {
        super(ap, IMixinValidator.ValidationPass.LATE);
    }

    @Override
    public boolean validate(TypeElement mixin, IAnnotationHandle annotation, Collection<TypeHandle> targets) {
        if ("true".equalsIgnoreCase(this.options.getOption("disableTargetValidator"))) {
            return true;
        }
        if (mixin.getKind() == ElementKind.INTERFACE) {
            this.validateInterfaceMixin(mixin, targets);
        } else {
            this.validateClassMixin(mixin, targets);
        }
        return true;
    }

    private void validateInterfaceMixin(TypeElement mixin, Collection<TypeHandle> targets) {
        boolean containsNonAccessorMethod = false;
        for (Element element : mixin.getEnclosedElements()) {
            if (element.getKind() != ElementKind.METHOD) continue;
            boolean isAccessor = AnnotationHandle.of(element, Accessor.class).exists();
            boolean isInvoker = AnnotationHandle.of(element, Invoker.class).exists();
            containsNonAccessorMethod |= !isAccessor && !isInvoker;
        }
        if (!containsNonAccessorMethod) {
            return;
        }
        for (TypeHandle typeHandle : targets) {
            if (typeHandle == null || !typeHandle.isNotInterface()) continue;
            this.messager.printMessage(IMessagerEx.MessageType.TARGET_VALIDATOR, (CharSequence)("Targetted type '" + typeHandle + " of " + mixin + " is not an interface"), (Element)mixin);
        }
    }

    private void validateClassMixin(TypeElement mixin, Collection<TypeHandle> targets) {
        TypeHandle superClass = this.typeHandleProvider.getTypeHandle(TypeUtils.getInternalName(mixin)).getSuperclass();
        for (TypeHandle target : targets) {
            if (target == null || this.validateSuperClass(target, superClass)) continue;
            this.messager.printMessage(IMessagerEx.MessageType.TARGET_VALIDATOR, (CharSequence)("Superclass " + superClass + " of " + mixin + " was not found in the hierarchy of target class " + target), (Element)mixin);
        }
    }

    private boolean validateSuperClass(TypeHandle targetType, TypeHandle superClass) {
        return targetType.isImaginary() || targetType.isSimulated() || superClass.isSuperTypeOf(targetType) || this.checkMixinsFor(targetType, superClass);
    }

    private boolean checkMixinsFor(TypeHandle targetType, TypeHandle superMixin) {
        IAnnotationHandle annotation = superMixin.getAnnotation(Mixin.class);
        for (Object target : annotation.getList()) {
            if (!this.typeHandleProvider.getTypeHandle(target).isSuperTypeOf(targetType)) continue;
            return true;
        }
        for (Object target : annotation.getList("targets")) {
            if (!this.typeHandleProvider.getTypeHandle((String)target).isSuperTypeOf(targetType)) continue;
            return true;
        }
        return false;
    }
}

