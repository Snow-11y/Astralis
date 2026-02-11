package com.llamalad7.mixinextras.transformer;

import com.llamalad7.mixinextras.expression.impl.point.ExpressionSliceMarkerTransformer;
import com.llamalad7.mixinextras.expression.impl.wrapper.ExpressionInjectorWrapperTransformer;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import com.llamalad7.mixinextras.sugar.impl.SugarMixinTransformer;
import com.llamalad7.mixinextras.transformer.MixinTransformer;
import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapperMixinTransformer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

public class MixinTransformerExtension
implements IExtension {
    private final Set<ClassNode> preparedMixins = Collections.newSetFromMap(new WeakHashMap());
    private final List<MixinTransformer> transformers = Arrays.asList(new ExpressionSliceMarkerTransformer(), new FactoryRedirectWrapperMixinTransformer(), new SugarMixinTransformer(), new ExpressionInjectorWrapperTransformer());

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
        for (Pair<IMixinInfo, ClassNode> pair : MixinInternals.getMixinsFor(context)) {
            IMixinInfo info = pair.getLeft();
            ClassNode node = pair.getRight();
            if (this.preparedMixins.contains(node)) continue;
            for (MixinTransformer transformer : this.transformers) {
                transformer.transform(info, node);
            }
            this.preparedMixins.add(node);
        }
    }

    @Override
    public void postApply(ITargetClassContext context) {
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }
}

