package org.spongepowered.asm.service;

public interface IMixinAuditTrail {
    public void onApply(String var1, String var2);

    public void onPostProcess(String var1);

    public void onGenerate(String var1, String var2);
}

