package com.llamalad7.mixinextras.sugar.impl.handlers;

import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import com.llamalad7.mixinextras.sugar.impl.handlers.HandlerInfo;
import com.llamalad7.mixinextras.sugar.impl.handlers.LocalHandlerTransformer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public abstract class HandlerTransformer {
    private static final Map<String, Class<? extends HandlerTransformer>> MAP = new HashMap<String, Class<? extends HandlerTransformer>>();
    protected final IMixinInfo mixin;
    protected final SugarParameter parameter;

    HandlerTransformer(IMixinInfo mixin, SugarParameter parameter) {
        this.mixin = mixin;
        this.parameter = parameter;
    }

    public abstract boolean isRequired(MethodNode var1);

    public abstract void transform(HandlerInfo var1);

    public static HandlerTransformer create(IMixinInfo mixin, SugarParameter parameter) {
        try {
            Class<? extends HandlerTransformer> clazz = MAP.get(parameter.sugar.desc);
            if (clazz == null) {
                return null;
            }
            Constructor<? extends HandlerTransformer> ctor = clazz.getDeclaredConstructor(IMixinInfo.class, SugarParameter.class);
            return ctor.newInstance(mixin, parameter);
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        List<Pair> sugars = Arrays.asList(Pair.of(Local.class, LocalHandlerTransformer.class));
        for (Pair pair : sugars) {
            for (String name : MixinExtrasService.getInstance().getAllClassNames(((Class)pair.getLeft()).getName())) {
                MAP.put('L' + name.replace('.', '/') + ';', (Class)pair.getRight());
            }
        }
    }
}

