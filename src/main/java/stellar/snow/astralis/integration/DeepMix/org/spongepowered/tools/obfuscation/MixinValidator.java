package org.spongepowered.tools.obfuscation;

import java.util.Collection;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.AnnotatedMixins;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerSuppressible;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IOptionProvider;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

public abstract class MixinValidator
implements IMixinValidator {
    protected final ProcessingEnvironment processingEnv;
    protected final IMessagerSuppressible messager;
    protected final IOptionProvider options;
    protected final ITypeHandleProvider typeHandleProvider;
    protected final IMixinValidator.ValidationPass pass;

    public MixinValidator(IMixinAnnotationProcessor ap, IMixinValidator.ValidationPass pass) {
        this.processingEnv = ap.getProcessingEnvironment();
        this.messager = ap;
        this.options = ap;
        this.typeHandleProvider = ap.getTypeProvider();
        this.pass = pass;
    }

    @Override
    public final boolean validate(IMixinValidator.ValidationPass pass, TypeElement mixin, IAnnotationHandle annotation, Collection<TypeHandle> targets) {
        if (pass != this.pass) {
            return true;
        }
        return this.validate(mixin, annotation, targets);
    }

    protected abstract boolean validate(TypeElement var1, IAnnotationHandle var2, Collection<TypeHandle> var3);

    protected final Collection<TypeHandle> getMixinsTargeting(TypeHandle targetType) {
        return AnnotatedMixins.getMixinsForEnvironment(this.processingEnv).getMixinsTargeting(targetType);
    }
}

