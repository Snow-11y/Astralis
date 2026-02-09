package org.spongepowered.asm.mixin.injection.struct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.IMessageSink;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.primitives.Ints;

public class InjectionPointData {
    private static final Pattern AT_PATTERN = InjectionPointData.createPattern();
    private final Map<String, String> args = new HashMap<String, String>();
    private final IInjectionPointContext context;
    private final String at;
    private final String type;
    private final InjectionPoint.Specifier specifier;
    private final InjectionPoint.RestrictTargetLevel targetRestriction;
    private final String target;
    private final String slice;
    private final int ordinal;
    private final int opcode;
    private final String id;
    private final int flags;

    public InjectionPointData(IInjectionPointContext context, String at, List<String> args, String target, String slice, int ordinal, int opcode, String id, int flags) {
        this.context = context;
        this.at = at;
        this.target = target;
        this.slice = Strings.nullToEmpty(slice);
        this.ordinal = Math.max(-1, ordinal);
        this.opcode = opcode;
        this.id = id;
        this.flags = flags;
        this.parseArgs(args);
        this.args.put("target", target);
        this.args.put("ordinal", String.valueOf(ordinal));
        this.args.put("opcode", String.valueOf(opcode));
        Matcher matcher = AT_PATTERN.matcher(at);
        this.type = InjectionPointData.parseType(matcher, at);
        this.specifier = InjectionPointData.parseSpecifier(matcher);
        this.targetRestriction = this.isUnsafe() ? InjectionPoint.RestrictTargetLevel.ALLOW_ALL : InjectionPoint.RestrictTargetLevel.METHODS_ONLY;
    }

    private void parseArgs(List<String> args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if (arg == null) continue;
            int eqPos = arg.indexOf(61);
            if (eqPos > -1) {
                this.args.put(arg.substring(0, eqPos), arg.substring(eqPos + 1));
                continue;
            }
            this.args.put(arg, "");
        }
    }

    public IMessageSink getMessageSink() {
        return this.context;
    }

    public String getAt() {
        return this.at;
    }

    public String getType() {
        return this.type;
    }

    public InjectionPoint.Specifier getSpecifier() {
        return this.specifier;
    }

    public InjectionPoint.RestrictTargetLevel getTargetRestriction() {
        return this.targetRestriction;
    }

    public IInjectionPointContext getContext() {
        return this.context;
    }

    public IMixinContext getMixin() {
        return this.context.getMixin();
    }

    public MethodNode getMethod() {
        return this.context.getMethod();
    }

    public Type getMethodReturnType() {
        return Type.getReturnType((String)this.getMethod().desc);
    }

    public AnnotationNode getParent() {
        return this.context.getAnnotationNode();
    }

    public String getSlice() {
        return this.slice;
    }

    public LocalVariableDiscriminator getLocalVariableDiscriminator() {
        return LocalVariableDiscriminator.parse(this.getParent());
    }

    public String get(String key, String defaultValue) {
        String value = this.args.get(key);
        return value != null ? value : defaultValue;
    }

    public int get(String key, int defaultValue) {
        return InjectionPointData.parseInt(this.get(key, String.valueOf(defaultValue)), defaultValue);
    }

    public boolean get(String key, boolean defaultValue) {
        return InjectionPointData.parseBoolean(this.get(key, String.valueOf(defaultValue)), defaultValue);
    }

    public <T extends Enum<T>> T get(String key, T defaultValue) {
        return InjectionPointData.parseEnum(this.get(key, defaultValue.name()), defaultValue);
    }

    public ITargetSelector get(String key) {
        try {
            return TargetSelector.parseAndValidate(this.get(key, ""), (ISelectorContext)this.context);
        }
        catch (InvalidSelectorException ex) {
            throw new InvalidInjectionPointException(this.getMixin(), (Throwable)ex, "Failed parsing @At(\"%s\").%s \"%s\" on %s", this.at, key, this.target, this.getDescription());
        }
    }

    public ITargetSelector getTarget() {
        try {
            String id;
            IAnnotationHandle selectorAnnotation;
            AnnotationNode desc;
            if (Strings.isNullOrEmpty(this.target) && (desc = (AnnotationNode)Annotations.getValue(((Annotations.Handle)(selectorAnnotation = this.context.getSelectorAnnotation())).getNode(), "desc")) != null && "at".equalsIgnoreCase(id = Annotations.getValue(desc, "id", "at"))) {
                return DynamicSelectorDesc.of(Annotations.handleOf(desc), this.context);
            }
            return TargetSelector.parseAndValidate(this.target, (ISelectorContext)this.context);
        }
        catch (InvalidSelectorException ex) {
            throw new InvalidInjectionPointException(this.getMixin(), (Throwable)ex, "Failed validating @At(\"%s\").target \"%s\" on %s", this.at, this.target, this.getDescription());
        }
    }

    public String getDescription() {
        return InjectionInfo.describeInjector(this.context.getMixin(), this.context.getAnnotationNode(), this.context.getMethod());
    }

    public int getOrdinal() {
        return this.ordinal;
    }

    public int getOpcode() {
        return this.opcode;
    }

    public int getOpcode(int defaultOpcode) {
        return this.opcode > 0 ? this.opcode : defaultOpcode;
    }

    public int getOpcode(int defaultOpcode, int ... validOpcodes) {
        for (int validOpcode : validOpcodes) {
            if (this.opcode != validOpcode) continue;
            return this.opcode;
        }
        return defaultOpcode;
    }

    public int[] getOpcodeList(String key, int[] defaultValue) {
        String[] values;
        String value = this.args.get(key);
        if (value == null) {
            return defaultValue;
        }
        TreeSet<Integer> parsed = new TreeSet<Integer>();
        for (String strOpcode : values = value.split("[ ,;]")) {
            int opcode = Bytecode.parseOpcodeName(strOpcode.trim());
            if (opcode <= 0) continue;
            parsed.add(opcode);
        }
        return Ints.toArray(parsed);
    }

    public String getId() {
        return this.id;
    }

    public boolean isUnsafe() {
        return (this.flags & 1) != 0;
    }

    public String toString() {
        return this.type;
    }

    private static Pattern createPattern() {
        return Pattern.compile(String.format("^(.+?)(:(%s))?$", Joiner.on('|').join((Object[])InjectionPoint.Specifier.values())));
    }

    public static String parseType(String at) {
        Matcher matcher = AT_PATTERN.matcher(at);
        return InjectionPointData.parseType(matcher, at);
    }

    private static String parseType(Matcher matcher, String at) {
        return matcher.matches() ? matcher.group(1) : at;
    }

    private static InjectionPoint.Specifier parseSpecifier(Matcher matcher) {
        return matcher.matches() && matcher.group(3) != null ? InjectionPoint.Specifier.valueOf(matcher.group(3)) : InjectionPoint.Specifier.DEFAULT;
    }

    private static int parseInt(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        }
        catch (Exception ex) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String string, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(string);
        }
        catch (Exception ex) {
            return defaultValue;
        }
    }

    private static <T extends Enum<T>> T parseEnum(String string, T defaultValue) {
        try {
            return (T)Enum.valueOf(defaultValue.getClass(), string);
        }
        catch (Exception ex) {
            return defaultValue;
        }
    }
}

