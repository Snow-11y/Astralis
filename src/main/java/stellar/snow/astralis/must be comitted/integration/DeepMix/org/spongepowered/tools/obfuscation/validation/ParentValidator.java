package org.spongepowered.tools.obfuscation.validation;

import java.util.Collection;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.MixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

public class ParentValidator
extends MixinValidator {
    public ParentValidator(IMixinAnnotationProcessor ap) {
        super(ap, IMixinValidator.ValidationPass.EARLY);
    }

    @Override
    public boolean validate(TypeElement mixin, IAnnotationHandle annotation, Collection<TypeHandle> targets) {
        if (mixin.getEnclosingElement().getKind() != ElementKind.PACKAGE && !mixin.getModifiers().contains((Object)Modifier.STATIC)) {
            this.messager.printMessage(IMessagerEx.MessageType.PARENT_VALIDATOR, (CharSequence)"Inner class mixin must be declared static", (Element)mixin);
        }
        return true;
    }
}

