package com.llamalad7.mixinextras.ap;

import com.llamalad7.mixinextras.ap.StdoutMessager;
import com.llamalad7.mixinextras.ap.expressions.DefinitionInfo;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Definitions;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.expression.Expressions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionV1InjectionInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethodInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.utils.MixinAPInternals;
import com.llamalad7.mixinextras.utils.MixinAPVersion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;

public class MixinExtrasAP
extends AbstractProcessor {
    private static final boolean MIXIN = MixinExtrasAP.setupMixin();
    private final List<DefinitionInfo> definitions = new ArrayList<DefinitionInfo>();

    private static boolean setupMixin() {
        try {
            MessageRouter.setMessager(new StdoutMessager());
        }
        catch (NoClassDefFoundError e) {
            return false;
        }
        return true;
    }

    private static void registerInjectors() {
        InjectionInfo.register(ModifyExpressionValueInjectionInfo.class);
        InjectionInfo.register(ModifyReceiverInjectionInfo.class);
        InjectionInfo.register(ModifyReturnValueInjectionInfo.class);
        InjectionInfo.register(WrapMethodInjectionInfo.class);
        InjectionInfo.register(WrapOperationInjectionInfo.class);
        InjectionInfo.register(WrapWithConditionV1InjectionInfo.class);
        InjectionInfo.register(WrapWithConditionInjectionInfo.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!MIXIN) {
            return false;
        }
        MixinAPVersion.check(this.processingEnv);
        if (roundEnv.processingOver()) {
            this.remapDefinitions();
            return true;
        }
        this.gatherDefinitions(roundEnv);
        return true;
    }

    private void gatherDefinitions(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Definition.class)) {
            AnnotationHandle def = AnnotationHandle.of(element, Definition.class);
            this.registerDefinition(element, def);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Definitions.class)) {
            AnnotationHandle defs = AnnotationHandle.of(element, Definitions.class);
            for (AnnotationHandle annotationHandle : defs.getAnnotationList("value")) {
                this.registerDefinition(element, annotationHandle);
            }
        }
    }

    private void registerDefinition(Element handler, AnnotationHandle def) {
        TypeElement mixin = (TypeElement)handler.getEnclosingElement();
        AnnotationHandle injector = this.getInjectorAnnotation(handler);
        Boolean remap = (Boolean)def.getValue("remap");
        for (String method : def.getList("method")) {
            this.definitions.add(new DefinitionInfo.Method(this.processingEnv, mixin, (ExecutableElement)handler, injector, method, remap));
        }
        for (String field : def.getList("field")) {
            this.definitions.add(new DefinitionInfo.Field(this.processingEnv, mixin, (ExecutableElement)handler, injector, field, remap));
        }
    }

    private void remapDefinitions() {
        for (DefinitionInfo def : this.definitions) {
            def.remap();
        }
        MixinAPInternals.writeReferences(this.processingEnv);
    }

    private AnnotationHandle getInjectorAnnotation(Element handler) {
        return InjectionInfo.getRegisteredAnnotations().stream().map(it -> AnnotationHandle.of(handler, it)).filter(AnnotationHandle::exists).findFirst().orElseThrow(() -> new IllegalStateException("Could not find injector annotation on " + handler));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        List<Class> types = Arrays.asList(Expression.class, Expressions.class, Definition.class, Definitions.class);
        return types.stream().map(Class::getName).collect(Collectors.toSet());
    }

    static {
        if (MIXIN) {
            MixinExtrasAP.registerInjectors();
        }
    }
}

