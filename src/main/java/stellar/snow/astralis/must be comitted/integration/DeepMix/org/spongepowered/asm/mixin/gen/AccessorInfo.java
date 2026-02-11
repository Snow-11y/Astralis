package org.spongepowered.asm.mixin.gen;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.AccessorGenerator;
import org.spongepowered.asm.mixin.gen.AccessorGeneratorFieldGetter;
import org.spongepowered.asm.mixin.gen.AccessorGeneratorFieldSetter;
import org.spongepowered.asm.mixin.gen.AccessorGeneratorMethodProxy;
import org.spongepowered.asm.mixin.gen.AccessorGeneratorObjectFactory;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.InvokerInfo;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;

public class AccessorInfo
extends SpecialMethodInfo {
    protected final Class<? extends Annotation> annotationClass;
    protected final Type[] argTypes;
    protected final Type returnType;
    protected final boolean isStatic;
    protected final String specifiedName;
    protected final AccessorType type;
    private final Type targetFieldType;
    protected final ITargetSelector target;
    protected FieldNode targetField;
    protected MethodNode targetMethod;
    protected AccessorGenerator generator;

    public AccessorInfo(MixinTargetContext mixin, MethodNode method) {
        this(mixin, method, Accessor.class);
    }

    protected AccessorInfo(MixinTargetContext mixin, MethodNode method, Class<? extends Annotation> annotationClass) {
        super(mixin, method, Annotations.getVisible(method, annotationClass));
        this.annotationClass = annotationClass;
        this.argTypes = Type.getArgumentTypes((String)method.desc);
        this.returnType = Type.getReturnType((String)method.desc);
        this.isStatic = Bytecode.isStatic(method);
        this.specifiedName = (String)Annotations.getValue(this.annotation);
        this.type = this.initType();
        this.targetFieldType = this.initTargetFieldType();
        this.target = this.initTarget();
        this.annotation.visit("target", (Object)this.target.toString());
    }

    protected AccessorType initType() {
        if (this.returnType.equals((Object)Type.VOID_TYPE)) {
            return AccessorType.FIELD_SETTER;
        }
        return AccessorType.FIELD_GETTER;
    }

    protected Type initTargetFieldType() {
        switch (this.type) {
            case FIELD_GETTER: {
                if (this.argTypes.length > 0) {
                    throw new InvalidAccessorException((IMixinContext)this.mixin, this + " must take exactly 0 arguments, found " + this.argTypes.length);
                }
                return this.returnType;
            }
            case FIELD_SETTER: {
                if (this.argTypes.length != 1) {
                    throw new InvalidAccessorException((IMixinContext)this.mixin, this + " must take exactly 1 argument, found " + this.argTypes.length);
                }
                return this.argTypes[0];
            }
        }
        throw new InvalidAccessorException((IMixinContext)this.mixin, "Computed unsupported accessor type " + (Object)((Object)this.type) + " for " + this);
    }

    protected ITargetSelector initTarget() {
        return new MemberInfo(this.getTargetName(this.specifiedName), null, this.targetFieldType.getDescriptor());
    }

    protected String getTargetName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            String inflectedTarget = this.inflectTarget();
            if (inflectedTarget == null) {
                throw new InvalidAccessorException((IMixinContext)this.mixin, String.format("Failed to inflect target name for %s, supported prefixes: %s", this, this.type.getExpectedPrefixes()));
            }
            return inflectedTarget;
        }
        return TargetSelector.parseName(name, this);
    }

    protected String inflectTarget() {
        return AccessorInfo.inflectTarget(MethodNodeEx.getName(this.method), this.type, this.toString(), (ISelectorContext)this, this.mixin.getEnvironment().getOption(MixinEnvironment.Option.DEBUG_VERBOSE));
    }

    public static String inflectTarget(String name, AccessorType type, String description, ISelectorContext context, boolean verbose) {
        return AccessorInfo.inflectTarget(AccessorName.of(name), type, description, context, verbose);
    }

    public static String inflectTarget(AccessorName name, AccessorType type, String description, ISelectorContext context, boolean verbose) {
        if (name != null) {
            if (!type.isExpectedPrefix(name.prefix) && verbose) {
                MixinService.getService().getLogger("mixin").warn("Unexpected prefix for {}, found [{}] expecting {}", description, name.prefix, type.getExpectedPrefixes());
            }
            return TargetSelector.parseName(name.name, context);
        }
        return null;
    }

    public final ITargetSelector getTarget() {
        return this.target;
    }

    public final Type getTargetFieldType() {
        return this.targetFieldType;
    }

    public final FieldNode getTargetField() {
        return this.targetField;
    }

    public final MethodNode getTargetMethod() {
        return this.targetMethod;
    }

    public final Type getReturnType() {
        return this.returnType;
    }

    public final Type[] getArgTypes() {
        return this.argTypes;
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    public String toString() {
        String typeString = this.type != null ? this.type.toString() : "UNPARSED_ACCESSOR";
        return String.format("%s->@%s[%s]::%s%s", this.mixin, Annotations.getSimpleName(this.annotation), typeString, this.methodName, this.method.desc);
    }

    public void locate() {
        this.targetField = this.findTargetField();
    }

    public void validate() {
        this.generator = this.type.getGenerator(this);
        this.generator.validate();
    }

    public MethodNode generate() {
        MethodNode generatedAccessor = this.generator.generate();
        Annotations.merge(this.method, generatedAccessor);
        return generatedAccessor;
    }

    private FieldNode findTargetField() {
        return (FieldNode)this.findTarget(ElementNode.fieldList(this.classNode));
    }

    protected <TNode> TNode findTarget(List<ElementNode<TNode>> nodes) {
        TargetSelector.Result<TNode> result = TargetSelector.run(this.target.configure(ITargetSelector.Configure.ORPHAN, new String[0]), nodes);
        try {
            return result.getSingleResult(true);
        }
        catch (IllegalStateException ex) {
            throw new InvalidAccessorException(this, String.format("%s matching %s in %s for %s", ex.getMessage(), this.target, this.classNode.name, this));
        }
    }

    public static AccessorInfo of(MixinTargetContext mixin, MethodNode method, Class<? extends Annotation> type) {
        if (type == Accessor.class) {
            return new AccessorInfo(mixin, method);
        }
        if (type == Invoker.class) {
            return new InvokerInfo(mixin, method);
        }
        throw new InvalidAccessorException((IMixinContext)mixin, "Could not parse accessor for unknown type " + type.getName());
    }

    public static final class AccessorName {
        private static final Pattern PATTERN = Pattern.compile("^(" + AccessorName.getPrefixList() + ")(([A-Z])(.*?))(_\\$md.*)?$");
        public final String methodName;
        public final String prefix;
        public final String name;

        private AccessorName(String methodName, String prefix, String name) {
            this.methodName = methodName;
            this.prefix = prefix;
            this.name = name;
        }

        public String toString() {
            return super.toString();
        }

        public static AccessorName of(String methodName) {
            return AccessorName.of(methodName, true);
        }

        public static AccessorName of(String methodName, boolean toMemberCase) {
            Matcher nameMatcher = PATTERN.matcher(methodName);
            if (nameMatcher.matches()) {
                String prefix = nameMatcher.group(1);
                String namePart = nameMatcher.group(2);
                String firstChar = nameMatcher.group(3);
                String remainder = nameMatcher.group(4);
                boolean nameIsUpperCase = AccessorName.isUpperCase(Locale.ROOT, namePart);
                String name = String.format("%s%s", AccessorName.toLowerCaseIf(Locale.ROOT, firstChar, toMemberCase && !nameIsUpperCase), remainder);
                return new AccessorName(methodName, prefix, name);
            }
            return null;
        }

        private static boolean isUpperCase(Locale locale, String string) {
            return string.toUpperCase(locale).equals(string);
        }

        private static String toLowerCaseIf(Locale locale, String string, boolean condition) {
            return condition ? string.toLowerCase(locale) : string;
        }

        private static String getPrefixList() {
            ArrayList<String> prefixes = new ArrayList<String>();
            for (AccessorType type : AccessorType.values()) {
                prefixes.addAll(type.getExpectedPrefixes());
            }
            return Joiner.on('|').join(prefixes);
        }
    }

    public static enum AccessorType {
        FIELD_GETTER(ImmutableSet.of("get", "is")){

            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorFieldGetter(info);
            }
        }
        ,
        FIELD_SETTER(ImmutableSet.of("set")){

            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorFieldSetter(info);
            }
        }
        ,
        METHOD_PROXY(ImmutableSet.of("call", "invoke")){

            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorMethodProxy(info);
            }
        }
        ,
        OBJECT_FACTORY(ImmutableSet.of("new", "create")){

            @Override
            AccessorGenerator getGenerator(AccessorInfo info) {
                return new AccessorGeneratorObjectFactory(info);
            }
        };

        private final Set<String> expectedPrefixes;

        private AccessorType(Set<String> expectedPrefixes) {
            this.expectedPrefixes = expectedPrefixes;
        }

        public boolean isExpectedPrefix(String prefix) {
            return this.expectedPrefixes.contains(prefix);
        }

        public Set<String> getExpectedPrefixes() {
            return Collections.unmodifiableSet(this.expectedPrefixes);
        }

        abstract AccessorGenerator getGenerator(AccessorInfo var1);
    }
}

