package org.spongepowered.tools.obfuscation;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.tools.obfuscation.AnnotatedMixins;
import org.spongepowered.tools.obfuscation.SupportedOptions;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;

abstract class MixinObfuscationProcessor
extends AbstractProcessor {
    protected AnnotatedMixins mixins;

    MixinObfuscationProcessor() {
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.mixins = AnnotatedMixins.getMixinsForEnvironment(processingEnv);
    }

    protected void processMixins(RoundEnvironment roundEnv) {
        this.mixins.onPassStarted();
        for (Element element : roundEnv.getElementsAnnotatedWith(Mixin.class)) {
            if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
                this.mixins.registerMixin((TypeElement)element);
                continue;
            }
            this.mixins.printMessage(IMessagerEx.MessageType.MIXIN_ON_INVALID_TYPE, (CharSequence)"Found an @Mixin annotation on an element which is not a class or interface", element);
        }
    }

    protected void postProcess(RoundEnvironment roundEnv) {
        this.mixins.onPassCompleted(roundEnv);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return SupportedOptions.getAllOptions();
    }
}

