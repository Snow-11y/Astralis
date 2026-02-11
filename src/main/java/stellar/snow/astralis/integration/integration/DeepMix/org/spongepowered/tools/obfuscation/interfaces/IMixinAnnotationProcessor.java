package org.spongepowered.tools.obfuscation.interfaces;

import javax.annotation.processing.ProcessingEnvironment;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.tools.obfuscation.interfaces.IJavadocProvider;
import org.spongepowered.tools.obfuscation.interfaces.IMessagerSuppressible;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.IOptionProvider;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;

public interface IMixinAnnotationProcessor
extends IMessagerSuppressible,
IOptionProvider {
    public CompilerEnvironment getCompilerEnvironment();

    public ProcessingEnvironment getProcessingEnvironment();

    public IObfuscationManager getObfuscationManager();

    public ITokenProvider getTokenProvider();

    public ITypeHandleProvider getTypeProvider();

    public IJavadocProvider getJavadocProvider();

    public static enum CompilerEnvironment {
        JAVAC(false, "Java Compiler"),
        JDT(true, "Eclipse (JDT)"){

            @Override
            protected boolean isDetected(ProcessingEnvironment processingEnv) {
                return processingEnv.getClass().getName().contains("jdt");
            }
        }
        ,
        IDEA(true, "IntelliJ IDEA"){

            @Override
            protected boolean isDetected(ProcessingEnvironment processingEnv) {
                for (String ideaSystemProperty : new String[]{"idea.plugins.path", "idea.config.path", "idea.home.path", "idea.paths.selector"}) {
                    if (System.getProperty(ideaSystemProperty) == null) continue;
                    return true;
                }
                return false;
            }
        };

        private final boolean isDevelopmentEnvironment;
        private final String friendlyName;

        private CompilerEnvironment(boolean isDevelopmentEnvironment, String friendlyName) {
            this.isDevelopmentEnvironment = isDevelopmentEnvironment;
            this.friendlyName = friendlyName;
        }

        public boolean isCompiler() {
            return !this.isDevelopmentEnvironment;
        }

        public boolean isDevelopmentEnvironment() {
            return this.isDevelopmentEnvironment;
        }

        public String getFriendlyName() {
            return this.friendlyName;
        }

        protected boolean isDetected(ProcessingEnvironment processingEnv) {
            return false;
        }

        public static CompilerEnvironment detect(ProcessingEnvironment processingEnv) {
            for (CompilerEnvironment environment : CompilerEnvironment.values()) {
                if (!environment.isDetected(processingEnv)) continue;
                return environment;
            }
            return JAVAC;
        }
    }
}

