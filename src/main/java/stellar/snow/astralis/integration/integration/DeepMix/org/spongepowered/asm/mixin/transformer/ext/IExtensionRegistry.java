package org.spongepowered.asm.mixin.transformer.ext;

import java.util.List;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

public interface IExtensionRegistry {
    public List<IExtension> getExtensions();

    public List<IExtension> getActiveExtensions();

    public <T extends IExtension> T getExtension(Class<? extends IExtension> var1);

    public ISyntheticClassRegistry getSyntheticClassRegistry();
}

