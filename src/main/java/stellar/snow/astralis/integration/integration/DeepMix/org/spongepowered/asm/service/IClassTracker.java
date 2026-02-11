package org.spongepowered.asm.service;

public interface IClassTracker {
    public void registerInvalidClass(String var1);

    public boolean isClassLoaded(String var1);

    public String getClassRestrictions(String var1);
}

