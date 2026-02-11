package org.spongepowered.tools.obfuscation;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.tools.obfuscation.MixinObfuscationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerEx;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

public class MixinObfuscationProcessorInjection
extends MixinObfuscationProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportedAnnotationTypes = new HashSet<String>();
        supportedAnnotationTypes.add(At.class.getName());
        for (Class<? extends Annotation> annotationType : InjectionInfo.getRegisteredAnnotations()) {
            supportedAnnotationTypes.add(annotationType.getName());
        }
        return supportedAnnotationTypes;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            this.postProcess(roundEnv);
            return true;
        }
        this.processMixins(roundEnv);
        for (Class<? extends Annotation> annotationType : InjectionInfo.getRegisteredAnnotations()) {
            this.processInjectors(roundEnv, annotationType);
        }
        this.postProcess(roundEnv);
        return true;
    }

    @Override
    protected void postProcess(RoundEnvironment roundEnv) {
        super.postProcess(roundEnv);
        try {
            this.mixins.writeReferences();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void processInjectors(RoundEnvironment roundEnv, Class<? extends Annotation> injectorClass) {
        for (Element element : roundEnv.getElementsAnnotatedWith(injectorClass)) {
            Element parent = element.getEnclosingElement();
            if (!(parent instanceof TypeElement)) {
                throw new IllegalStateException("@" + injectorClass.getSimpleName() + " element has unexpected parent with type " + TypeUtils.getElementType(parent));
            }
            AnnotationHandle inject = AnnotationHandle.of(element, injectorClass);
            if (element.getKind() == ElementKind.METHOD) {
                this.mixins.registerInjector((TypeElement)parent, (ExecutableElement)element, inject);
                continue;
            }
            this.mixins.printMessage(IMessagerEx.MessageType.INJECTOR_ON_NON_METHOD_ELEMENT, (CharSequence)("Found an @" + injectorClass.getSimpleName() + " annotation on an element which is not a method: " + element.toString()));
        }
    }
}

