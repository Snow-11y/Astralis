package org.spongepowered.asm.service;

import org.spongepowered.asm.service.IPropertyKey;

public interface IGlobalPropertyService {
    public IPropertyKey resolveKey(String var1);

    public <T> T getProperty(IPropertyKey var1);

    public void setProperty(IPropertyKey var1, Object var2);

    public <T> T getProperty(IPropertyKey var1, T var2);

    public String getPropertyString(IPropertyKey var1, String var2);
}

