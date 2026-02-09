package org.spongepowered.asm.mixin.refmap;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Type;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IClassReferenceMapper;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Quantifier;

public final class RemappingReferenceMapper
implements IClassReferenceMapper,
IReferenceMapper {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private final IReferenceMapper refMap;
    private final IRemapper remapper;
    private final Map<String, String> mappedReferenceCache = new HashMap<String, String>();

    private RemappingReferenceMapper(MixinEnvironment env, IReferenceMapper refMap) {
        this.refMap = refMap;
        this.remapper = env.getRemappers();
        logger.debug("Remapping refMap {} using remapper chain", refMap.getResourceName());
    }

    @Override
    public boolean isDefault() {
        return this.refMap.isDefault();
    }

    @Override
    public String getResourceName() {
        return this.refMap.getResourceName();
    }

    @Override
    public String getStatus() {
        return this.refMap.getStatus();
    }

    @Override
    public String getContext() {
        return this.refMap.getContext();
    }

    @Override
    public void setContext(String context) {
        this.refMap.setContext(context);
    }

    @Override
    public String remap(String className, String reference) {
        return this.remapWithContext(this.getContext(), className, reference);
    }

    private static String remapMethodDescriptor(IRemapper remapper, String desc) {
        StringBuilder newDesc = new StringBuilder();
        newDesc.append('(');
        for (Type arg : Type.getArgumentTypes((String)desc)) {
            newDesc.append(remapper.mapDesc(arg.getDescriptor()));
        }
        return newDesc.append(')').append(remapper.mapDesc(Type.getReturnType((String)desc).getDescriptor())).toString();
    }

    @Override
    public String remapWithContext(String context, String className, String reference) {
        if (reference.isEmpty()) {
            return reference;
        }
        String origInfoString = this.refMap.remapWithContext(context, className, reference);
        String remappedCached = this.mappedReferenceCache.get(origInfoString);
        if (remappedCached != null) {
            return remappedCached;
        }
        String remapped = origInfoString;
        MemberInfo info = MemberInfo.parse(remapped, null);
        if (info.getName() == null && info.getDesc() == null) {
            return info.getOwner() != null ? new MemberInfo(this.remapper.map(info.getOwner()), Quantifier.DEFAULT).toString() : info.toString();
        }
        remapped = info.isField() ? new MemberInfo(this.remapper.mapFieldName(info.getOwner(), info.getName(), info.getDesc()), info.getOwner() == null ? null : this.remapper.map(info.getOwner()), info.getDesc() == null ? null : this.remapper.mapDesc(info.getDesc())).toString() : new MemberInfo(this.remapper.mapMethodName(info.getOwner(), info.getName(), info.getDesc()), info.getOwner() == null ? null : this.remapper.map(info.getOwner()), info.getDesc() == null ? null : RemappingReferenceMapper.remapMethodDescriptor(this.remapper, info.getDesc())).toString();
        this.mappedReferenceCache.put(origInfoString, remapped);
        return remapped;
    }

    public static IReferenceMapper of(MixinEnvironment env, IReferenceMapper refMap) {
        if (!refMap.isDefault()) {
            return new RemappingReferenceMapper(env, refMap);
        }
        return refMap;
    }

    @Override
    public String remapClassName(String className, String inputClassName) {
        return this.remapClassNameWithContext(this.getContext(), className, inputClassName);
    }

    @Override
    public String remapClassNameWithContext(String context, String className, String remapped) {
        String origInfoString = this.refMap instanceof IClassReferenceMapper ? ((IClassReferenceMapper)((Object)this.refMap)).remapClassNameWithContext(context, className, remapped) : this.refMap.remapWithContext(context, className, remapped);
        return this.remapper.map(origInfoString.replace('.', '/'));
    }
}

