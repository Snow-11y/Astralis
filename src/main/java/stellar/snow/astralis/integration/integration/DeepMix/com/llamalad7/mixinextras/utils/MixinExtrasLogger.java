package com.llamalad7.mixinextras.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

public interface MixinExtrasLogger {
    public void warn(String var1, Object ... var2);

    public void info(String var1, Object ... var2);

    public void debug(String var1, Object ... var2);

    public void error(String var1, Throwable var2);

    public static MixinExtrasLogger get(String name) {
        Object impl;
        try {
            IMixinService service = MixinService.getService();
            Method getLogger = service.getClass().getMethod("getLogger", String.class);
            impl = getLogger.invoke(service, "MixinExtras|" + name);
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e1) {
            try {
                impl = Class.forName("org.apache.logging.log4j.LogManager").getMethod("getLogger", String.class).invoke(null, name);
            }
            catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e2) {
                IllegalStateException e = new IllegalStateException("Could not get logger! Please inform LlamaLad7!");
                e.addSuppressed(e1);
                e.addSuppressed(e2);
                throw e;
            }
        }
        Object finalImpl = impl;
        return (MixinExtrasLogger)Proxy.newProxyInstance(MixinExtrasLogger.class.getClassLoader(), new Class[]{MixinExtrasLogger.class}, (proxy, method, args) -> finalImpl.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(finalImpl, args));
    }
}

