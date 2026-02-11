package org.spongepowered.tools.obfuscation.interfaces;

import java.util.Collection;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;

public interface IObfuscationEnvironment {
    public MappingMethod getObfMethod(ITargetSelectorRemappable var1);

    public MappingMethod getObfMethod(MappingMethod var1);

    public MappingMethod getObfMethod(MappingMethod var1, boolean var2);

    public MappingField getObfField(ITargetSelectorRemappable var1);

    public MappingField getObfField(MappingField var1);

    public MappingField getObfField(MappingField var1, boolean var2);

    public String getObfClass(String var1);

    public ITargetSelectorRemappable remapDescriptor(ITargetSelectorRemappable var1);

    public String remapDescriptor(String var1);

    public void writeMappings(Collection<IMappingConsumer> var1);
}

