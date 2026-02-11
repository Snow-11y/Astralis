package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.ModUtil;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Counter;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.primitives.Chars;

class MethodMapper {
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private static final List<String> classes = new ArrayList<String>();
    private static final Map<String, Counter> methods = new HashMap<String, Counter>();
    private final ClassInfo info;
    private int nextUniqueMethodIndex;
    private int nextUniqueFieldIndex;

    public MethodMapper(MixinEnvironment env, ClassInfo info) {
        this.info = info;
    }

    public void reset() {
        this.nextUniqueMethodIndex = 0;
        this.nextUniqueFieldIndex = 0;
    }

    public void remapHandlerMethod(MixinInfo mixin, MethodNode handler, ClassInfo.Method method) {
        if (!(handler instanceof MixinInfo.MixinMethodNode) || !((MixinInfo.MixinMethodNode)handler).isInjector()) {
            return;
        }
        if (method.isUnique()) {
            logger.warn("Redundant @Unique on injector method {} in {}. Injectors are implicitly unique", method, mixin);
        }
        if (method.isRenamed()) {
            handler.name = method.getName();
            return;
        }
        String handlerName = this.getHandlerName(mixin, (MixinInfo.MixinMethodNode)handler);
        handler.name = method.conform(handlerName);
    }

    public String getHandlerName(MixinInfo mixin, MixinInfo.MixinMethodNode method) {
        String prefix = InjectionInfo.getInjectorPrefix(method.getInjectorAnnotation());
        String classUID = MethodMapper.getClassUID(method.getOwner().getClassRef());
        String mod = MethodMapper.getMixinSourceId(mixin, "");
        String methodName = method.name;
        if (!mod.isEmpty()) {
            if (methodName.startsWith(mod) && methodName.length() > mod.length() + 1 && Chars.contains(new char[]{'_', '$'}, methodName.charAt(mod.length()))) {
                methodName = methodName.substring(mod.length() + 1);
            }
            mod = mod + '$';
        }
        String methodUID = MethodMapper.getMethodUID(methodName, method.desc, !method.isSurrogate());
        return String.format("%s$%s%s$%s%s", prefix, classUID, methodUID, mod, methodName);
    }

    public String getUniqueName(MixinInfo mixin, MethodNode method, String sessionId, boolean preservePrefix) {
        String mod;
        String uniqueIndex = Integer.toHexString(this.nextUniqueMethodIndex++);
        String methodName = method.name;
        if (method instanceof MethodNodeEx && !(mod = MethodMapper.getMixinSourceId(mixin, "")).isEmpty()) {
            if (methodName.startsWith(mod) && methodName.length() > mod.length() + 1 && Chars.contains(new char[]{'_', '$'}, methodName.charAt(mod.length()))) {
                methodName = methodName.substring(mod.length() + 1);
            }
            methodName = preservePrefix ? methodName + '$' + mod : mod + '$' + methodName;
        }
        String pattern = preservePrefix ? "%2$s_$md$%1$s$%3$s" : "md%s$%s$%s";
        return String.format(pattern, sessionId.substring(30), methodName, uniqueIndex);
    }

    public String getUniqueName(MixinInfo mixin, FieldNode field, String sessionId) {
        String uniqueIndex = Integer.toHexString(this.nextUniqueFieldIndex++);
        return String.format("fd%s$%s%s$%s", sessionId.substring(30), MethodMapper.getMixinSourceId(mixin, "$"), field.name, uniqueIndex);
    }

    private static String getMixinSourceId(MixinInfo mixin, String separator) {
        String sourceId = mixin.getConfig().getCleanSourceId();
        if (sourceId == null) {
            String modId = ModUtil.owner(mixin.getConfig(), null);
            if (modId == null) {
                return "";
            }
            return modId + separator;
        }
        if (sourceId.length() > 12) {
            sourceId = sourceId.substring(0, 12);
        }
        return String.format("%s%s", sourceId, separator);
    }

    private static String getClassUID(String classRef) {
        int index = classes.indexOf(classRef);
        if (index < 0) {
            index = classes.size();
            classes.add(classRef);
        }
        return MethodMapper.finagle(index);
    }

    private static String getMethodUID(String name, String desc, boolean increment) {
        String descriptor = String.format("%s%s", name, desc);
        Counter id = methods.get(descriptor);
        if (id == null) {
            id = new Counter();
            methods.put(descriptor, id);
        } else if (increment) {
            ++id.value;
        }
        return String.format("%03x", id.value);
    }

    private static String finagle(int index) {
        String hex = Integer.toHexString(index);
        StringBuilder sb = new StringBuilder();
        for (int pos = 0; pos < hex.length(); ++pos) {
            char c;
            c = (char)(c + ((c = hex.charAt(pos)) < ':' ? 49 : 10));
            sb.append(c);
        }
        return Strings.padStart(sb.toString(), 3, 'z');
    }
}

