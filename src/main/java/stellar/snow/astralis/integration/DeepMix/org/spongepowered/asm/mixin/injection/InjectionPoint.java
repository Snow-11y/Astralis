package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.modify.AfterStoreLocal;
import org.spongepowered.asm.mixin.injection.modify.BeforeLoadLocal;
import org.spongepowered.asm.mixin.injection.points.AfterInvoke;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeFinalReturn;
import org.spongepowered.asm.mixin.injection.points.BeforeInvoke;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.points.BeforeStringInvoke;
import org.spongepowered.asm.mixin.injection.points.ConstructorHead;
import org.spongepowered.asm.mixin.injection.points.JumpInsnPoint;
import org.spongepowered.asm.mixin.injection.points.MethodHead;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointAnnotationContext;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.IMessageSink;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.collect.ImmutableList;

public abstract class InjectionPoint {
    public static final int DEFAULT_ALLOWED_SHIFT_BY = 0;
    public static final int MAX_ALLOWED_SHIFT_BY = 5;
    private static Map<String, Class<? extends InjectionPoint>> types = new HashMap<String, Class<? extends InjectionPoint>>();
    private final String slice;
    private final Specifier specifier;
    private final String id;
    private final IMessageSink messageSink;
    private RestrictTargetLevel targetRestriction;

    protected InjectionPoint() {
        this("", Specifier.DEFAULT, null);
    }

    protected InjectionPoint(InjectionPointData data) {
        this(data.getSlice(), data.getSpecifier(), data.getId(), data.getMessageSink(), data.getTargetRestriction());
    }

    public InjectionPoint(String slice, Specifier specifier, String id) {
        this(slice, specifier, id, null);
    }

    public InjectionPoint(String slice, Specifier specifier, String id, IMessageSink messageSink) {
        this(slice, specifier, id, messageSink, RestrictTargetLevel.METHODS_ONLY);
    }

    public InjectionPoint(String slice, Specifier specifier, String id, IMessageSink messageSink, RestrictTargetLevel targetRestriction) {
        this.slice = slice;
        this.specifier = specifier;
        this.id = id;
        this.messageSink = messageSink;
        this.targetRestriction = targetRestriction;
    }

    public String getSlice() {
        return this.slice;
    }

    public Specifier getSpecifier(Specifier defaultSpecifier) {
        return this.specifier == Specifier.DEFAULT ? defaultSpecifier : this.specifier;
    }

    public String getId() {
        return this.id;
    }

    protected void addMessage(String format, Object ... args) {
        if (this.messageSink != null) {
            this.messageSink.addMessage(format, args);
        }
    }

    public boolean checkPriority(int targetPriority, int mixinPriority) {
        return targetPriority < mixinPriority;
    }

    protected void setTargetRestriction(RestrictTargetLevel targetRestriction) {
        this.targetRestriction = targetRestriction;
    }

    public RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
        return this.targetRestriction;
    }

    public abstract boolean find(String var1, InsnList var2, Collection<AbstractInsnNode> var3);

    public String toString() {
        return String.format("@At(\"%s\")", this.getAtCode());
    }

    protected static AbstractInsnNode nextNode(InsnList insns, AbstractInsnNode insn) {
        int index = insns.indexOf(insn) + 1;
        if (index > 0 && index < insns.size()) {
            return insns.get(index);
        }
        return insn;
    }

    public static InjectionPoint and(InjectionPoint ... operands) {
        return new Intersection(operands);
    }

    public static InjectionPoint or(InjectionPoint ... operands) {
        return new Union(operands);
    }

    public static InjectionPoint after(InjectionPoint point) {
        return new Shift(point, 1);
    }

    public static InjectionPoint before(InjectionPoint point) {
        return new Shift(point, -1);
    }

    public static InjectionPoint shift(InjectionPoint point, int count) {
        return new Shift(point, count);
    }

    public static List<InjectionPoint> parse(IMixinContext context, MethodNode method, AnnotationNode parent, List<AnnotationNode> ats) {
        return InjectionPoint.parse((IInjectionPointContext)new AnnotatedMethodInfo(context, method, parent), ats);
    }

    public static List<InjectionPoint> parse(IInjectionPointContext context, List<AnnotationNode> ats) {
        ImmutableList.Builder injectionPoints = ImmutableList.builder();
        for (AnnotationNode at : ats) {
            InjectionPoint injectionPoint = InjectionPoint.parse((IInjectionPointContext)new InjectionPointAnnotationContext(context, at, "at"), at);
            if (injectionPoint == null) continue;
            injectionPoints.add(injectionPoint);
        }
        return injectionPoints.build();
    }

    public static InjectionPoint parse(IInjectionPointContext context, At at) {
        return InjectionPoint.parse(context, at.value(), at.shift(), at.by(), Arrays.asList(at.args()), at.target(), at.slice(), at.ordinal(), at.opcode(), at.id(), Flags.parse(at));
    }

    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, At at) {
        return InjectionPoint.parse(new AnnotatedMethodInfo(context, method, parent), at.value(), at.shift(), at.by(), Arrays.asList(at.args()), at.target(), at.slice(), at.ordinal(), at.opcode(), at.id(), Flags.parse(at));
    }

    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, AnnotationNode at) {
        return InjectionPoint.parse((IInjectionPointContext)new InjectionPointAnnotationContext((IInjectionPointContext)new AnnotatedMethodInfo(context, method, parent), at, "at"), at);
    }

    public static InjectionPoint parse(IInjectionPointContext context, AnnotationNode at) {
        String value = (String)Annotations.getValue(at, "value");
        ImmutableList<String> args = (ImmutableList<String>)Annotations.getValue(at, "args");
        String target = Annotations.getValue(at, "target", "");
        String slice = Annotations.getValue(at, "slice", "");
        At.Shift shift = Annotations.getValue(at, "shift", At.Shift.class, At.Shift.NONE);
        int by = Annotations.getValue(at, "by", 0);
        int ordinal = Annotations.getValue(at, "ordinal", -1);
        int opcode = Annotations.getValue(at, "opcode", 0);
        String id = (String)Annotations.getValue(at, "id");
        int flags = Flags.parse(at);
        if (args == null) {
            args = ImmutableList.of();
        }
        return InjectionPoint.parse(context, value, shift, by, args, target, slice, ordinal, opcode, id, flags);
    }

    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, String at, At.Shift shift, int by, List<String> args, String target, String slice, int ordinal, int opcode, String id, int flags) {
        return InjectionPoint.parse(new AnnotatedMethodInfo(context, method, parent), at, shift, by, args, target, slice, ordinal, opcode, id, flags);
    }

    public static InjectionPoint parse(IInjectionPointContext context, String at, At.Shift shift, int by, List<String> args, String target, String slice, int ordinal, int opcode, String id, int flags) {
        InjectionPointData data = new InjectionPointData(context, at, args, target, slice, ordinal, opcode, id, flags);
        Class<? extends InjectionPoint> ipClass = InjectionPoint.findClass(context.getMixin(), data);
        InjectionPoint point = InjectionPoint.create(context.getMixin(), data, ipClass);
        return InjectionPoint.shift(context, point, shift, by);
    }

    private static Class<? extends InjectionPoint> findClass(IMixinContext context, InjectionPointData data) {
        String type = data.getType();
        Class<InjectionPoint> ipClass = types.get(type.toUpperCase(Locale.ROOT));
        if (ipClass == null) {
            if (type.matches("^([A-Za-z_][A-Za-z0-9_]*[\\.\\$])+[A-Za-z_][A-Za-z0-9_]*$")) {
                try {
                    ipClass = MixinService.getService().getClassProvider().findClass(type);
                    types.put(type, ipClass);
                }
                catch (Exception ex) {
                    throw new InvalidInjectionException(context, data + " could not be loaded or is not a valid InjectionPoint", (Throwable)ex);
                }
            } else {
                throw new InvalidInjectionException(context, data + " is not a valid injection point specifier");
            }
        }
        return ipClass;
    }

    private static InjectionPoint create(IMixinContext context, InjectionPointData data, Class<? extends InjectionPoint> ipClass) {
        Constructor<? extends InjectionPoint> ipCtor = null;
        try {
            ipCtor = ipClass.getDeclaredConstructor(InjectionPointData.class);
            ipCtor.setAccessible(true);
        }
        catch (NoSuchMethodException ex) {
            throw new InvalidInjectionException(context, ipClass.getName() + " must contain a constructor which accepts an InjectionPointData", (Throwable)ex);
        }
        InjectionPoint point = null;
        try {
            point = ipCtor.newInstance(data);
        }
        catch (InvocationTargetException ex) {
            throw new InvalidInjectionException(context, "Error whilst instancing injection point " + ipClass.getName() + " for " + data.getAt(), ex.getCause());
        }
        catch (Exception ex) {
            throw new InvalidInjectionException(context, "Error whilst instancing injection point " + ipClass.getName() + " for " + data.getAt(), (Throwable)ex);
        }
        return point;
    }

    private static InjectionPoint shift(IInjectionPointContext context, InjectionPoint point, At.Shift shift, int by) {
        if (point != null) {
            if (shift == At.Shift.BEFORE) {
                return InjectionPoint.before(point);
            }
            if (shift == At.Shift.AFTER) {
                return InjectionPoint.after(point);
            }
            if (shift == At.Shift.BY) {
                InjectionPoint.validateByValue(context.getMixin(), context.getMethod(), context.getAnnotationNode(), point, by);
                return InjectionPoint.shift(point, by);
            }
        }
        return point;
    }

    private static void validateByValue(IMixinContext context, MethodNode method, AnnotationNode parent, InjectionPoint point, int by) {
        MixinEnvironment env = context.getMixin().getConfig().getEnvironment();
        ShiftByViolationBehaviour err = env.getOption(MixinEnvironment.Option.SHIFT_BY_VIOLATION_BEHAVIOUR, ShiftByViolationBehaviour.WARN);
        if (err == ShiftByViolationBehaviour.IGNORE) {
            return;
        }
        String limitBreached = "the maximum allowed value: ";
        String advice = "Increase the value of maxShiftBy to suppress this warning.";
        int allowed = 0;
        if (context instanceof MixinTargetContext) {
            allowed = ((MixinTargetContext)context).getMaxShiftByValue();
        }
        if (by <= allowed) {
            return;
        }
        if (by > 5) {
            limitBreached = "MAX_ALLOWED_SHIFT_BY=";
            advice = "You must use an alternate query or a custom injection point.";
            allowed = 5;
        }
        String message = String.format("@%s(%s) Shift.BY=%d on %s::%s exceeds %s%d. %s", Annotations.getSimpleName(parent), point, by, context, method.name, limitBreached, allowed, advice);
        if (err == ShiftByViolationBehaviour.WARN && allowed < 5) {
            MixinService.getService().getLogger("mixin").warn(message, new Object[0]);
            return;
        }
        throw new InvalidInjectionException(context, message);
    }

    protected String getAtCode() {
        AtCode code = this.getClass().getAnnotation(AtCode.class);
        return code == null ? this.getClass().getName() : code.value().toUpperCase();
    }

    @Deprecated
    public static void register(Class<? extends InjectionPoint> type) {
        InjectionPoint.register(type, null);
    }

    public static void register(Class<? extends InjectionPoint> type, String namespace) {
        Class<? extends InjectionPoint> existing;
        AtCode code = type.getAnnotation(AtCode.class);
        if (code == null) {
            throw new IllegalArgumentException("Injection point class " + type + " is not annotated with @AtCode");
        }
        String annotationNamespace = code.namespace();
        if (!Strings.isNullOrEmpty(annotationNamespace)) {
            namespace = annotationNamespace;
        }
        if ((existing = types.get(code.value())) != null && !existing.equals(type)) {
            MixinService.getService().getLogger("mixin").debug("Overriding InjectionPoint {} with {} (previously {})", code.value(), type.getName(), existing.getName());
        } else if (Strings.isNullOrEmpty(namespace)) {
            MixinService.getService().getLogger("mixin").warn("Registration of InjectionPoint {} with {} without specifying namespace is deprecated.", code.value(), type.getName());
        }
        String id = code.value().toUpperCase(Locale.ROOT);
        if (!Strings.isNullOrEmpty(namespace)) {
            id = namespace.toUpperCase(Locale.ROOT) + ":" + id;
        }
        types.put(id, type);
    }

    private static void registerBuiltIn(Class<? extends InjectionPoint> type) {
        String code = type.getAnnotation(AtCode.class).value().toUpperCase(Locale.ROOT);
        types.put(code, type);
        types.put("MIXIN:" + code, type);
    }

    static {
        InjectionPoint.registerBuiltIn(BeforeFieldAccess.class);
        InjectionPoint.registerBuiltIn(BeforeInvoke.class);
        InjectionPoint.registerBuiltIn(BeforeNew.class);
        InjectionPoint.registerBuiltIn(BeforeReturn.class);
        InjectionPoint.registerBuiltIn(BeforeStringInvoke.class);
        InjectionPoint.registerBuiltIn(JumpInsnPoint.class);
        InjectionPoint.registerBuiltIn(MethodHead.class);
        InjectionPoint.registerBuiltIn(AfterInvoke.class);
        InjectionPoint.registerBuiltIn(BeforeLoadLocal.class);
        InjectionPoint.registerBuiltIn(AfterStoreLocal.class);
        InjectionPoint.registerBuiltIn(BeforeFinalReturn.class);
        InjectionPoint.registerBuiltIn(BeforeConstant.class);
        InjectionPoint.registerBuiltIn(ConstructorHead.class);
    }

    static final class Shift
    extends InjectionPoint {
        private final InjectionPoint input;
        private final int shift;

        public Shift(InjectionPoint input, int shift) {
            if (input == null) {
                throw new IllegalArgumentException("Must supply an input injection point for SHIFT");
            }
            this.input = input;
            this.shift = shift;
        }

        @Override
        public String toString() {
            return "InjectionPoint(" + this.getClass().getSimpleName() + ")[" + this.input + "]";
        }

        @Override
        public RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
            return this.input.getTargetRestriction(context);
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            List<Object> list = nodes instanceof List ? (List<Object>)nodes : new ArrayList<AbstractInsnNode>(nodes);
            this.input.find(desc, insns, nodes);
            ListIterator<AbstractInsnNode> iter = list.listIterator();
            while (iter.hasNext()) {
                int sourceIndex = insns.indexOf((AbstractInsnNode)iter.next());
                int newIndex = sourceIndex + this.shift;
                if (newIndex >= 0 && newIndex < insns.size()) {
                    iter.set(insns.get(newIndex));
                    continue;
                }
                iter.remove();
                int absShift = Math.abs(this.shift);
                char operator = absShift != this.shift ? (char)'-' : '+';
                this.input.addMessage("@At.shift offset outside the target bounds: Index (index(%d) %s offset(%d) = %d) is outside the allowed range (0-%d)", sourceIndex, Character.valueOf(operator), absShift, newIndex, insns.size());
            }
            if (nodes != list) {
                nodes.clear();
                nodes.addAll((Collection<AbstractInsnNode>)list);
            }
            return nodes.size() > 0;
        }
    }

    static final class Union
    extends CompositeInjectionPoint {
        public Union(InjectionPoint ... points) {
            super(points);
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            LinkedHashSet<AbstractInsnNode> allNodes = new LinkedHashSet<AbstractInsnNode>();
            for (int i = 0; i < this.components.length; ++i) {
                this.components[i].find(desc, insns, allNodes);
            }
            nodes.addAll(allNodes);
            return allNodes.size() > 0;
        }
    }

    static final class Intersection
    extends CompositeInjectionPoint {
        public Intersection(InjectionPoint ... points) {
            super(points);
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            boolean found = false;
            ArrayList[] allNodes = (ArrayList[])Array.newInstance(ArrayList.class, this.components.length);
            for (int i = 0; i < this.components.length; ++i) {
                allNodes[i] = new ArrayList();
                this.components[i].find(desc, insns, allNodes[i]);
            }
            ArrayList alpha = allNodes[0];
            for (int nodeIndex = 0; nodeIndex < alpha.size(); ++nodeIndex) {
                AbstractInsnNode node = (AbstractInsnNode)alpha.get(nodeIndex);
                boolean in = true;
                for (int b = 1; b < allNodes.length && allNodes[b].contains(node); ++b) {
                }
                if (!in) continue;
                nodes.add(node);
                found = true;
            }
            return found;
        }
    }

    static abstract class CompositeInjectionPoint
    extends InjectionPoint {
        protected final InjectionPoint[] components;

        protected CompositeInjectionPoint(InjectionPoint ... components) {
            if (components == null || components.length < 2) {
                throw new IllegalArgumentException("Must supply two or more component injection points for composite point!");
            }
            this.components = components;
        }

        @Override
        public RestrictTargetLevel getTargetRestriction(IInjectionPointContext context) {
            RestrictTargetLevel level = RestrictTargetLevel.METHODS_ONLY;
            for (InjectionPoint component : this.components) {
                RestrictTargetLevel componentLevel = component.getTargetRestriction(context);
                if (componentLevel.ordinal() <= level.ordinal()) continue;
                level = componentLevel;
            }
            return level;
        }

        @Override
        public String toString() {
            return "CompositeInjectionPoint(" + this.getClass().getSimpleName() + ")[" + Joiner.on(',').join(this.components) + "]";
        }
    }

    public static final class Flags {
        public static final int UNSAFE = 1;

        public static int parse(At at) {
            int flags = 0;
            if (at.unsafe()) {
                flags |= 1;
            }
            return flags;
        }

        public static int parse(AnnotationNode at) {
            int flags = 0;
            if (Annotations.getValue(at, "unsafe", Boolean.TRUE).booleanValue()) {
                flags |= 1;
            }
            return flags;
        }
    }

    static enum ShiftByViolationBehaviour {
        IGNORE,
        WARN,
        ERROR;

    }

    public static enum RestrictTargetLevel {
        METHODS_ONLY,
        CONSTRUCTORS_AFTER_DELEGATE,
        ALLOW_ALL;

    }

    public static enum Specifier {
        ALL,
        FIRST,
        LAST,
        ONE,
        DEFAULT;

    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.TYPE})
    public static @interface AtCode {
        public String namespace() default "";

        public String value();
    }
}

