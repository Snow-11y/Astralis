package org.spongepowered.asm.mixin.transformer.ext.extensions;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.transformers.MixinClassWriter;

public class ExtensionCheckClass
implements IExtension {
    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return environment.getOption(MixinEnvironment.Option.DEBUG_VERIFY);
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
        try {
            context.getClassNode().accept((ClassVisitor)new CheckClassAdapter((ClassVisitor)new MixinClassWriter(2)));
        }
        catch (RuntimeException ex) {
            throw new ValidationFailedException(ex.getMessage(), ex);
        }
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }

    public static class ValidationFailedException
    extends MixinException {
        public ValidationFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public ValidationFailedException(String message) {
            super(message);
        }

        public ValidationFailedException(Throwable cause) {
            super(cause);
        }
    }
}

