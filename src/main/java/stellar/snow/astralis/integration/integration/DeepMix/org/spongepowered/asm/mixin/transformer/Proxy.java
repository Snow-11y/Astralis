package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.launchwrapper.IClassTransformer;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;

public final class Proxy
implements IClassTransformer,
ILegacyClassTransformer {
    public static List<Proxy> proxies = new ArrayList<Proxy>();
    public static MixinTransformer transformer = new MixinTransformer();
    private boolean isActive = true;

    public Proxy() {
        for (Proxy proxy : proxies) {
            proxy.isActive = false;
        }
        proxies.add(this);
        MixinService.getService().getLogger("mixin").debug("Adding new mixin transformer proxy #{}", proxies.size());
    }

    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return transformer.transformClassBytes(name, transformedName, basicClass);
        }
        return basicClass;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return transformer.transformClassBytes(name, transformedName, basicClass);
        }
        return basicClass;
    }
}

