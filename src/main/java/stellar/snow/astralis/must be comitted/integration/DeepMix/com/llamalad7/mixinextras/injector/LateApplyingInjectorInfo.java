package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.lib.apache.commons.ClassUtils;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.utils.MixinExtrasLogger;
import com.llamalad7.mixinextras.utils.ProxyUtils;
import java.lang.reflect.InvocationTargetException;

public interface LateApplyingInjectorInfo {
    public void lateInject();

    public void latePostInject();

    public void wrap(LateApplyingInjectorInfo var1);

    public String getLateInjectionType();

    @Deprecated
    default public void lateApply() {
        this.lateInject();
        MixinExtrasLogger logger = MixinExtrasLogger.get("Sugar");
        logger.warn("Skipping post injection checks for {} since it is from 0.2.0-beta.1 and cannot be saved", this);
    }

    public static boolean wrap(Object inner, LateApplyingInjectorInfo outer) {
        Class theirInterface = ClassUtils.getAllInterfaces(inner.getClass()).stream().filter(it -> it.getName().endsWith(".LateApplyingInjectorInfo")).findFirst().orElse(null);
        if (theirInterface == null || !MixinExtrasService.getInstance().isClassOwned(theirInterface.getName())) {
            return false;
        }
        try {
            inner.getClass().getMethod("wrap", theirInterface).invoke(inner, ProxyUtils.getProxy(outer, theirInterface));
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Failed to wrap InjectionInfo: ", e);
        }
        return true;
    }
}

