package com.llamalad7.mixinextras.service;

import com.llamalad7.mixinextras.service.MixinExtrasServiceImpl;
import com.llamalad7.mixinextras.utils.Blackboard;
import com.llamalad7.mixinextras.utils.ProxyUtils;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

public interface MixinExtrasService {
    public int getVersion();

    public boolean shouldReplace(Object var1);

    public void takeControlFrom(Object var1);

    public void concedeTo(Object var1, boolean var2);

    public void offerPackage(int var1, String var2);

    public void offerExtension(int var1, IExtension var2);

    public void offerInjector(int var1, Class<? extends InjectionInfo> var2);

    public void offerInjectionPoint(int var1, Class<? extends InjectionPoint> var2);

    public void initialize();

    public static void setup() {
        Object latestImpl = Blackboard.get("MixinExtrasServiceInstance");
        if (latestImpl == null) {
            MixinExtrasServiceImpl newImpl = new MixinExtrasServiceImpl();
            Blackboard.put("MixinExtrasServiceInstance", newImpl);
            newImpl.takeControlFrom(null);
            return;
        }
        MixinExtrasServiceImpl ourImpl = new MixinExtrasServiceImpl();
        if (ourImpl.shouldReplace(latestImpl)) {
            MixinExtrasService.getFrom(latestImpl).concedeTo(ourImpl, true);
            Blackboard.put("MixinExtrasServiceInstance", ourImpl);
            ourImpl.takeControlFrom(latestImpl);
        } else {
            ourImpl.concedeTo(latestImpl, false);
        }
    }

    public static MixinExtrasService getFrom(Object serviceImpl) {
        return ProxyUtils.getProxy(serviceImpl, MixinExtrasService.class);
    }

    public static MixinExtrasServiceImpl getInstance() {
        Object impl = Blackboard.get("MixinExtrasServiceInstance");
        if (impl instanceof MixinExtrasServiceImpl) {
            MixinExtrasServiceImpl ourImpl = (MixinExtrasServiceImpl)impl;
            if (ourImpl.initialized) {
                return ourImpl;
            }
            throw new IllegalStateException("Cannot use service because it is not initialized!");
        }
        throw new IllegalStateException("Cannot use service because another service is active: " + impl);
    }
}

