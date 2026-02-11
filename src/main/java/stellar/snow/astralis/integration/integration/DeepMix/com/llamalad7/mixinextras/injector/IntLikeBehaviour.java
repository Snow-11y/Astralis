package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import java.util.Arrays;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public abstract class IntLikeBehaviour {
    private IntLikeBehaviour() {
    }

    public Type transform(InjectionInfo info, Type expected, Type handler) {
        if (!expected.getDescriptor().contains(ExpressionASMUtils.INTLIKE_TYPE.getDescriptor())) {
            return expected;
        }
        return this.transformImpl(info, expected, handler);
    }

    protected abstract Type transformImpl(InjectionInfo var1, Type var2, Type var3);

    protected Type replaceIntLike(InjectionInfo info, Type desc, Type replacement) {
        if (!ExpressionASMUtils.isIntLike(replacement)) {
            throw CompatibilityHelper.makeInvalidInjectionException(info, String.format("Expected int-like type (boolean, byte, char, short, int), got %s", replacement));
        }
        Type returnType = desc.getReturnType();
        if (returnType.equals((Object)ExpressionASMUtils.INTLIKE_TYPE)) {
            returnType = replacement;
        }
        Type[] args = (Type[])Arrays.stream(desc.getArgumentTypes()).map(it -> it.equals((Object)ExpressionASMUtils.INTLIKE_TYPE) ? replacement : it).toArray(Type[]::new);
        return Type.getMethodType((Type)returnType, (Type[])args);
    }

    public static class MatchArgType
    extends IntLikeBehaviour {
        private final int index;

        public MatchArgType(int index) {
            this.index = index;
        }

        @Override
        public Type transformImpl(InjectionInfo info, Type expected, Type handler) {
            if (this.index >= handler.getArgumentTypes().length) {
                throw CompatibilityHelper.makeInvalidInjectionException(info, String.format("Expected int-like type for arg %s (boolean, byte, char, short, int), signature was %s", this.index, handler.getDescriptor()));
            }
            return this.replaceIntLike(info, expected, handler.getArgumentTypes()[this.index]);
        }
    }

    public static class MatchReturnType
    extends IntLikeBehaviour {
        public static final MatchReturnType INSTANCE = new MatchReturnType();

        @Override
        public Type transformImpl(InjectionInfo info, Type expected, Type handler) {
            return this.replaceIntLike(info, expected, handler.getReturnType());
        }
    }
}

