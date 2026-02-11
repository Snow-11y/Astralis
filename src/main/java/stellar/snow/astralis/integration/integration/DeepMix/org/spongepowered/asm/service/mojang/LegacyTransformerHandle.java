package org.spongepowered.asm.service.mojang;

import net.minecraft.launchwrapper.IClassTransformer;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

class LegacyTransformerHandle
implements ILegacyClassTransformer {
    private final IClassTransformer transformer;

    LegacyTransformerHandle(IClassTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public String getName() {
        return this.transformer.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        try {
            IClassProvider classProvider = MixinService.getService().getClassProvider();
            Class<?> clResource = classProvider.findClass("javax.annotation.Resource");
            return this.transformer.getClass().getAnnotation(clResource) != null;
        }
        catch (ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        return this.transformer.transform(name, transformedName, basicClass);
    }
}

