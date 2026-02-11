package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.injector.LateApplyingInjectorInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

public class LateInjectionApplicatorExtension
implements IExtension {
    private static final Map<ITargetClassContext, Map<String, List<Runnable[]>>> QUEUED_INJECTIONS = Collections.synchronizedMap(new HashMap());

    static void offerInjection(ITargetClassContext targetClassContext, LateApplyingInjectorInfo injectorInfo) {
        Map map = QUEUED_INJECTIONS.computeIfAbsent(targetClassContext, k -> LateInjectionApplicatorExtension.initializeMap());
        Runnable[] runnableArray = new Runnable[2];
        runnableArray[0] = injectorInfo::lateInject;
        runnableArray[1] = injectorInfo::latePostInject;
        ((List)map.get(injectorInfo.getLateInjectionType())).add(runnableArray);
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
        Map<String, List<Runnable[]>> relevant = QUEUED_INJECTIONS.get(context);
        if (relevant == null) {
            return;
        }
        for (List<Runnable[]> queuedInjections : relevant.values()) {
            for (Runnable[] injection : queuedInjections) {
                injection[0].run();
            }
            for (Runnable[] injection : queuedInjections) {
                injection[1].run();
            }
        }
        QUEUED_INJECTIONS.remove(context);
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }

    private static Map<String, List<Runnable[]>> initializeMap() {
        LinkedHashMap<String, List<Runnable[]>> result = new LinkedHashMap<String, List<Runnable[]>>();
        result.put("ModifyExpressionValue", new ArrayList());
        result.put("WrapWithCondition", new ArrayList());
        result.put("WrapOperation", new ArrayList());
        return result;
    }
}

