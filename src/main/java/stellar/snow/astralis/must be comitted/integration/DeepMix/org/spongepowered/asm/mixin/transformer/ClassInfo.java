package org.spongepowered.asm.mixin.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.transformer.MethodMapper;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.ClassSignature;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.asm.ClassNodeAdapter;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.include.com.google.common.base.Strings;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;

public final class ClassInfo {
    public static final int INCLUDE_PRIVATE = 2;
    public static final int INCLUDE_STATIC = 8;
    public static final int INCLUDE_ALL = 10;
    public static final int INCLUDE_INITIALISERS = 262144;
    private static final List<Callback> callbacks = new ArrayList<Callback>();
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private static final Profiler profiler = Profiler.getProfiler("meta");
    private static final Map<String, ClassInfo> cache = new HashMap<String, ClassInfo>();
    private static final ClassInfo OBJECT = new ClassInfo();
    private final String name;
    private final String superName;
    private final String outerName;
    private final boolean isInner;
    private final boolean isProbablyStatic;
    private final Set<String> interfaces;
    private final Set<Method> initialisers;
    private final Set<Method> methods;
    private final Set<Field> fields;
    private final Set<MixinInfo> mixins;
    private final Map<ClassInfo, ClassInfo> correspondingTypes = new HashMap<ClassInfo, ClassInfo>();
    private final MixinInfo mixin;
    private final MethodMapper methodMapper;
    private final boolean isMixin;
    private final boolean isInterface;
    private final int access;
    private ClassInfo superClass;
    private ClassInfo outerClass;
    private ClassSignature signature;
    private Set<MixinInfo> appliedMixins;
    private String nestHost;
    private Set<String> nestMembers;

    private ClassInfo() {
        this.name = "java/lang/Object";
        this.superName = null;
        this.outerName = null;
        this.isInner = false;
        this.isProbablyStatic = true;
        this.initialisers = ImmutableSet.of(new Method("<init>", "()V"));
        this.methods = ImmutableSet.of(new Method("getClass", "()Ljava/lang/Class;"), new Method("hashCode", "()I"), new Method("equals", "(Ljava/lang/Object;)Z"), new Method("clone", "()Ljava/lang/Object;"), new Method("toString", "()Ljava/lang/String;"), new Method("notify", "()V"), new Method[]{new Method("notifyAll", "()V"), new Method("wait", "(J)V"), new Method("wait", "(JI)V"), new Method("wait", "()V"), new Method("finalize", "()V")});
        this.fields = Collections.emptySet();
        this.isInterface = false;
        this.interfaces = Collections.emptySet();
        this.access = 1;
        this.isMixin = false;
        this.mixin = null;
        this.mixins = Collections.emptySet();
        this.methodMapper = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private ClassInfo(ClassNode classNode) {
        Profiler.Section timer = profiler.begin(1, "class.meta");
        try {
            this.name = classNode.name;
            this.superName = classNode.superName != null ? classNode.superName : "java/lang/Object";
            this.initialisers = new HashSet<Method>();
            this.methods = new HashSet<Method>();
            this.fields = new HashSet<Field>();
            this.isInterface = (classNode.access & 0x200) != 0;
            this.interfaces = new HashSet<String>();
            this.isMixin = classNode instanceof MixinInfo.MixinClassNode;
            this.mixin = this.isMixin ? ((MixinInfo.MixinClassNode)classNode).getMixin() : null;
            this.mixins = this.isMixin ? Collections.emptySet() : new HashSet();
            this.interfaces.addAll(classNode.interfaces);
            for (MethodNode method : classNode.methods) {
                this.addMethod(method, this.isMixin);
            }
            boolean isProbablyStatic = true;
            String outerName = classNode.outerClass;
            for (FieldNode field : classNode.fields) {
                if ((field.access & 0x1000) != 0 && field.name.startsWith("this$")) {
                    isProbablyStatic = false;
                    if (outerName == null && (outerName = field.desc) != null && outerName.startsWith("L") && outerName.endsWith(";")) {
                        outerName = outerName.substring(1, outerName.length() - 1);
                    }
                }
                this.fields.add(new Field(field, this.isMixin));
            }
            this.isProbablyStatic = isProbablyStatic;
            this.methodMapper = new MethodMapper(MixinEnvironment.getCurrentEnvironment(), this);
            this.signature = ClassSignature.ofLazy(classNode);
            int access = classNode.access;
            boolean isInner = outerName != null;
            for (InnerClassNode innerClass : classNode.innerClasses) {
                if (!this.name.equals(innerClass.name)) continue;
                access = innerClass.access;
                isInner = true;
                outerName = innerClass.outerName;
            }
            this.access = access;
            this.isInner = isInner;
            this.outerName = outerName;
            if (MixinEnvironment.getCompatibilityLevel().supports(8)) {
                this.nestHost = ClassNodeAdapter.getNestHostClass(classNode);
                List<String> nestMembers = ClassNodeAdapter.getNestMembers(classNode);
                if (nestMembers != null) {
                    this.nestMembers = new LinkedHashSet<String>();
                    this.nestMembers.addAll(nestMembers);
                }
            }
            for (Callback callback : callbacks) {
                callback.onInit(this);
            }
        }
        finally {
            timer.end();
        }
    }

    void addInterface(String iface) {
        this.interfaces.add(iface);
        this.getSignature().addInterface(iface);
    }

    void addMethod(MethodNode method) {
        this.addMethod(method, true);
    }

    private void addMethod(MethodNode method, boolean injected) {
        if (method.name.startsWith("<")) {
            this.initialisers.add(new Method(method, injected));
        } else {
            this.methods.add(new Method(method, injected));
        }
    }

    void addMixin(MixinInfo mixin) {
        if (this.isMixin) {
            throw new IllegalArgumentException("Cannot add target " + this.name + " for " + mixin.getClassName() + " because the target is a mixin");
        }
        this.mixins.add(mixin);
    }

    void addAppliedMixin(MixinInfo mixin) {
        if (this.appliedMixins == null) {
            this.appliedMixins = new HashSet<MixinInfo>();
        }
        this.appliedMixins.add(mixin);
    }

    public Set<IMixinInfo> getAppliedMixins() {
        return this.appliedMixins != null ? Collections.unmodifiableSet(this.appliedMixins) : Collections.emptySet();
    }

    public Set<IMixinInfo> getApplicableMixins() {
        return this.mixins.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(this.mixins);
    }

    public boolean isMixin() {
        return this.isMixin;
    }

    public boolean isLoadable() {
        return this.mixin != null && this.mixin.isLoadable();
    }

    public boolean isPublic() {
        return (this.access & 1) != 0;
    }

    public boolean isReallyPublic() {
        boolean isPublic = this.isPublic();
        if (!this.isInner || !isPublic) {
            return isPublic;
        }
        ClassInfo outer = this;
        while (outer != null && outer.outerName != null) {
            outer = ClassInfo.forName(outer.outerName);
            if (outer == null || outer.isPublic()) continue;
            return false;
        }
        return true;
    }

    public boolean isProtected() {
        return (this.access & 4) != 0;
    }

    public boolean isPrivate() {
        return (this.access & 2) != 0;
    }

    public boolean isAbstract() {
        return (this.access & 0x400) != 0;
    }

    public boolean isSynthetic() {
        return (this.access & 0x1000) != 0;
    }

    public boolean isProbablyStatic() {
        return this.isProbablyStatic;
    }

    public boolean isInner() {
        return this.isInner;
    }

    public boolean isInterface() {
        return this.isInterface;
    }

    public Set<String> getInterfaces() {
        return Collections.unmodifiableSet(this.interfaces);
    }

    public String toString() {
        return this.name;
    }

    MethodMapper getMethodMapper() {
        return this.methodMapper;
    }

    public int getAccess() {
        return this.access;
    }

    public String getName() {
        return this.name;
    }

    public String getClassName() {
        return this.name.replace('/', '.');
    }

    public String getSimpleName() {
        int pos = this.name.lastIndexOf(47);
        return pos < 0 ? this.name : this.name.substring(pos + 1);
    }

    public Type getType() {
        return Type.getObjectType((String)this.name);
    }

    public String getSuperName() {
        return this.superName;
    }

    public ClassInfo getSuperClass() {
        if (this.superClass == null && this.superName != null) {
            this.superClass = ClassInfo.forName(this.superName);
        }
        return this.superClass;
    }

    public String getOuterName() {
        return this.outerName;
    }

    public ClassInfo getOuterClass() {
        if (this.outerClass == null && this.outerName != null) {
            this.outerClass = ClassInfo.forName(this.outerName);
        }
        return this.outerClass;
    }

    public ClassSignature getSignature() {
        return this.signature.wake();
    }

    public String getNestHost() {
        return this.nestHost;
    }

    public Set<String> getNestMembers() {
        return this.nestMembers != null ? Collections.unmodifiableSet(this.nestMembers) : Collections.emptySet();
    }

    public ClassInfo resolveNestHost() {
        if (!Strings.isNullOrEmpty(this.nestHost)) {
            return ClassInfo.forName(this.nestHost);
        }
        return this;
    }

    List<ClassInfo> getTargets() {
        if (this.mixin != null) {
            ArrayList<ClassInfo> targets = new ArrayList<ClassInfo>();
            targets.add(this);
            targets.addAll(this.mixin.getTargets());
            return targets;
        }
        return ImmutableList.of(this);
    }

    public Set<Method> getMethods() {
        return Collections.unmodifiableSet(this.methods);
    }

    public Set<Method> getInterfaceMethods(boolean includeMixins) {
        HashSet<Method> methods = new HashSet<Method>();
        if (!this.isInterface) {
            for (ClassInfo supClass = this.addMethodsRecursive(methods, includeMixins); supClass != null && supClass != OBJECT; supClass = supClass.addMethodsRecursive(methods, includeMixins)) {
            }
        }
        Iterator it = methods.iterator();
        while (it.hasNext()) {
            if (((Method)it.next()).isAbstract()) continue;
            it.remove();
        }
        return Collections.unmodifiableSet(methods);
    }

    private ClassInfo addMethodsRecursive(Set<Method> methods, boolean includeMixins) {
        if (this.isInterface) {
            for (Method method : this.methods) {
                if (!method.isAbstract()) {
                    methods.remove(method);
                }
                methods.add(method);
            }
        } else if (!this.isMixin && includeMixins) {
            for (MixinInfo mixin : this.mixins) {
                mixin.getClassInfo().addMethodsRecursive(methods, includeMixins);
            }
        }
        for (String iface : this.interfaces) {
            ClassInfo classInfo = ClassInfo.forName(iface);
            if (classInfo == null) {
                logger.debug("Failed to resolve implementing interface {} on {}", iface, this.name);
                continue;
            }
            classInfo.addMethodsRecursive(methods, includeMixins);
        }
        return this.getSuperClass();
    }

    public boolean hasSuperClass(Class<?> superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE, superClass.isInterface());
    }

    public boolean hasSuperClass(Class<?> superClass, Traversal traversal) {
        return this.hasSuperClass(superClass, traversal, superClass.isInterface());
    }

    public boolean hasSuperClass(Class<?> superClass, Traversal traversal, boolean includeInterfaces) {
        String internalName = Type.getInternalName(superClass);
        if ("java/lang/Object".equals(internalName)) {
            return true;
        }
        return this.findSuperClass(internalName, traversal) != null;
    }

    public boolean hasSuperClass(String superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE, false);
    }

    public boolean hasSuperClass(String superClass, Traversal traversal) {
        return this.hasSuperClass(superClass, traversal, false);
    }

    public boolean hasSuperClass(String superClass, Traversal traversal, boolean includeInterfaces) {
        if ("java/lang/Object".equals(superClass)) {
            return true;
        }
        return this.findSuperClass(superClass, traversal) != null;
    }

    public boolean hasSuperClass(ClassInfo superClass) {
        return this.hasSuperClass(superClass, Traversal.NONE, false);
    }

    public boolean hasSuperClass(ClassInfo superClass, Traversal traversal) {
        return this.hasSuperClass(superClass, traversal, false);
    }

    public boolean hasSuperClass(ClassInfo superClass, Traversal traversal, boolean includeInterfaces) {
        if (OBJECT == superClass) {
            return true;
        }
        return this.findSuperClass(superClass.name, traversal, includeInterfaces) != null;
    }

    public ClassInfo findSuperClass(String superClass) {
        return this.findSuperClass(superClass, Traversal.NONE);
    }

    public ClassInfo findSuperClass(String superClass, Traversal traversal) {
        return this.findSuperClass(superClass, traversal, false, new HashSet<String>());
    }

    public ClassInfo findSuperClass(String superClass, Traversal traversal, boolean includeInterfaces) {
        if (ClassInfo.OBJECT.name.equals(superClass)) {
            return null;
        }
        return this.findSuperClass(superClass, traversal, includeInterfaces, new HashSet<String>());
    }

    private ClassInfo findSuperClass(String superClass, Traversal traversal, boolean includeInterfaces, Set<String> traversed) {
        ClassInfo iface;
        ClassInfo superClassInfo = this.getSuperClass();
        if (superClassInfo != null) {
            for (ClassInfo superTarget : superClassInfo.getTargets()) {
                if (superClass.equals(superTarget.getName())) {
                    return superClassInfo;
                }
                ClassInfo found = superTarget.findSuperClass(superClass, traversal.next(), includeInterfaces, traversed);
                if (found == null) continue;
                return found;
            }
        }
        if (includeInterfaces && (iface = this.findInterface(superClass)) != null) {
            return iface;
        }
        if (traversal.canTraverse()) {
            for (MixinInfo mixin : this.mixins) {
                String mixinClassName = mixin.getClassName();
                if (traversed.contains(mixinClassName)) continue;
                traversed.add(mixinClassName);
                ClassInfo mixinClass = mixin.getClassInfo();
                if (superClass.equals(mixinClass.getName())) {
                    return mixinClass;
                }
                ClassInfo targetSuper = mixinClass.findSuperClass(superClass, Traversal.ALL, includeInterfaces, traversed);
                if (targetSuper == null) continue;
                return targetSuper;
            }
        }
        return null;
    }

    private ClassInfo findInterface(String superClass) {
        for (String ifaceName : this.getInterfaces()) {
            ClassInfo iface = ClassInfo.forName(ifaceName);
            if (superClass.equals(ifaceName)) {
                return iface;
            }
            ClassInfo superIface = iface.findInterface(superClass);
            if (superIface == null) continue;
            return superIface;
        }
        return null;
    }

    ClassInfo findCorrespondingType(ClassInfo mixin) {
        if (mixin == null || !mixin.isMixin || this.isMixin) {
            return null;
        }
        ClassInfo correspondingType = this.correspondingTypes.get(mixin);
        if (correspondingType == null) {
            correspondingType = this.findSuperTypeForMixin(mixin);
            this.correspondingTypes.put(mixin, correspondingType);
        }
        return correspondingType;
    }

    private ClassInfo findSuperTypeForMixin(ClassInfo mixin) {
        for (ClassInfo superClass = this; superClass != null && superClass != OBJECT; superClass = superClass.getSuperClass()) {
            for (MixinInfo minion : superClass.mixins) {
                if (!minion.getClassInfo().equals(mixin)) continue;
                return superClass;
            }
        }
        return null;
    }

    public boolean hasMixinInHierarchy() {
        if (!this.isMixin) {
            return false;
        }
        for (ClassInfo supClass = this.getSuperClass(); supClass != null && supClass != OBJECT; supClass = supClass.getSuperClass()) {
            if (!supClass.isMixin) continue;
            return true;
        }
        return false;
    }

    public boolean hasMixinTargetInHierarchy() {
        if (this.isMixin) {
            return false;
        }
        for (ClassInfo supClass = this.getSuperClass(); supClass != null && supClass != OBJECT; supClass = supClass.getSuperClass()) {
            if (supClass.mixins.size() <= 0) continue;
            return true;
        }
        return false;
    }

    public Method findMethodInHierarchy(MethodNode method, SearchType searchType) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE);
    }

    public Method findMethodInHierarchy(MethodNode method, SearchType searchType, Traversal traversal) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, traversal, 0);
    }

    public Method findMethodInHierarchy(MethodNode method, SearchType searchType, int flags) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE, flags);
    }

    public Method findMethodInHierarchy(MethodNode method, SearchType searchType, Traversal traversal, int flags) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, traversal, flags);
    }

    public Method findMethodInHierarchy(MethodInsnNode method, SearchType searchType) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE);
    }

    public Method findMethodInHierarchy(MethodInsnNode method, SearchType searchType, int flags) {
        return this.findMethodInHierarchy(method.name, method.desc, searchType, Traversal.NONE, flags);
    }

    public Method findMethodInHierarchy(String name, String desc, SearchType searchType, int flags) {
        return this.findMethodInHierarchy(name, desc, searchType, Traversal.NONE, flags);
    }

    public Method findMethodInHierarchy(String name, String desc, SearchType searchType, Traversal traversal) {
        return this.findMethodInHierarchy(name, desc, searchType, traversal, 0);
    }

    public Method findMethodInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags) {
        return (Method)this.findInHierarchy(name, desc, searchType, traversal, flags, Member.Type.METHOD);
    }

    public Field findFieldInHierarchy(FieldNode field, SearchType searchType) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE);
    }

    public Field findFieldInHierarchy(FieldNode field, SearchType searchType, int flags) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE, flags);
    }

    public Field findFieldInHierarchy(FieldInsnNode field, SearchType searchType) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE);
    }

    public Field findFieldInHierarchy(FieldInsnNode field, SearchType searchType, int flags) {
        return this.findFieldInHierarchy(field.name, field.desc, searchType, Traversal.NONE, flags);
    }

    public Field findFieldInHierarchy(String name, String desc, SearchType searchType) {
        return this.findFieldInHierarchy(name, desc, searchType, Traversal.NONE);
    }

    public Field findFieldInHierarchy(String name, String desc, SearchType searchType, int flags) {
        return this.findFieldInHierarchy(name, desc, searchType, Traversal.NONE, flags);
    }

    public Field findFieldInHierarchy(String name, String desc, SearchType searchType, Traversal traversal) {
        return this.findFieldInHierarchy(name, desc, searchType, traversal, 0);
    }

    public Field findFieldInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags) {
        return (Field)this.findInHierarchy(name, desc, searchType, traversal, flags, Member.Type.FIELD);
    }

    private <M extends Member> M findInHierarchy(String name, String desc, SearchType searchType, Traversal traversal, int flags, Member.Type type) {
        ClassInfo superClassInfo;
        if (searchType == SearchType.ALL_CLASSES) {
            M member = this.findMember(name, desc, flags, type);
            if (member != null) {
                return member;
            }
            if (traversal.canTraverse()) {
                for (MixinInfo mixin : this.mixins) {
                    M mixinMember = mixin.getClassInfo().findMember(name, desc, flags, type);
                    if (mixinMember == null) continue;
                    return this.cloneMember(mixinMember);
                }
            }
        }
        if ((superClassInfo = this.getSuperClass()) != null) {
            for (ClassInfo superTarget : superClassInfo.getTargets()) {
                M member = superTarget.findInHierarchy(name, desc, SearchType.ALL_CLASSES, traversal.next(), flags & 0xFFFFFFFD, type);
                if (member == null) continue;
                return member;
            }
        }
        if (type == Member.Type.METHOD && (this.isInterface || MixinEnvironment.getCompatibilityLevel().supports(1))) {
            for (String implemented : this.interfaces) {
                ClassInfo iface = ClassInfo.forName(implemented);
                if (iface == null) {
                    logger.debug("Failed to resolve declared interface {} on {}", implemented, this.name);
                    continue;
                }
                M member = iface.findInHierarchy(name, desc, SearchType.ALL_CLASSES, traversal.next(), flags & 0xFFFFFFFD, type);
                if (member == null) continue;
                return (M)(this.isInterface ? member : new InterfaceMethod((Member)member));
            }
        }
        return null;
    }

    private <M extends Member> M cloneMember(M member) {
        if (member instanceof Method) {
            return (M)new Method(member);
        }
        return (M)new Field(member);
    }

    public Method findMethod(MethodNode method) {
        return this.findMethod(method.name, method.desc, method.access);
    }

    public Method findMethod(MethodNode method, int flags) {
        return this.findMethod(method.name, method.desc, flags);
    }

    public Method findMethod(MethodInsnNode method) {
        return this.findMethod(method.name, method.desc, 0);
    }

    public Method findMethod(MethodInsnNode method, int flags) {
        return this.findMethod(method.name, method.desc, flags);
    }

    public Method findMethod(String name, String desc, int flags) {
        return (Method)this.findMember(name, desc, flags, Member.Type.METHOD);
    }

    public Field findField(FieldNode field) {
        return this.findField(field.name, field.desc, field.access);
    }

    public Field findField(FieldInsnNode field, int flags) {
        return this.findField(field.name, field.desc, flags);
    }

    public Field findField(String name, String desc, int flags) {
        return (Field)this.findMember(name, desc, flags, Member.Type.FIELD);
    }

    private <M extends Member> M findMember(String name, String desc, int flags, Member.Type memberType) {
        Set<Member> members = memberType == Member.Type.METHOD ? this.methods : this.fields;
        for (Member member : members) {
            if (!member.equals(name, desc) || !member.matchesFlags(flags)) continue;
            return (M)member;
        }
        if (memberType == Member.Type.METHOD && (flags & 0x40000) != 0) {
            for (Method ctor : this.initialisers) {
                if (!ctor.equals(name, desc) || !ctor.matchesFlags(flags)) continue;
                return (M)ctor;
            }
        }
        return null;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ClassInfo)) {
            return false;
        }
        return ((ClassInfo)other).name.equals(this.name);
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    static ClassInfo fromClassNode(ClassNode classNode) {
        ClassInfo info = cache.get(classNode.name);
        if (info == null) {
            info = new ClassInfo(classNode);
            cache.put(classNode.name, info);
        }
        return info;
    }

    public static ClassInfo forName(String className) {
        ClassInfo info = cache.get(className = className.replace('.', '/'));
        if (info == null) {
            try {
                int flags = MixinEnvironment.getCurrentEnvironment().getOption(MixinEnvironment.Option.CLASSREADER_EXPAND_FRAMES) ? 8 : 0;
                ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(className, true, flags);
                info = new ClassInfo(classNode);
            }
            catch (Exception ex) {
                logger.catching(Level.TRACE, ex);
                logger.warn("Error loading class: {} ({}: {})", className, ex.getClass().getName(), ex.getMessage());
            }
            cache.put(className, info);
            logger.trace("Added class metadata for {} to metadata cache", className);
        }
        return info;
    }

    public static ClassInfo forDescriptor(String descriptor, TypeLookup lookup) {
        Type type;
        try {
            type = Type.getObjectType((String)descriptor);
        }
        catch (IllegalArgumentException ex) {
            logger.warn("Error resolving type from descriptor: {}", descriptor);
            return null;
        }
        return ClassInfo.forType(type, lookup);
    }

    public static ClassInfo forType(Type type, TypeLookup lookup) {
        if (type.getSort() == 9) {
            if (lookup == TypeLookup.ELEMENT_TYPE) {
                return ClassInfo.forType(type.getElementType(), TypeLookup.ELEMENT_TYPE);
            }
            return OBJECT;
        }
        if (type.getSort() < 9) {
            return null;
        }
        return ClassInfo.forName(type.getClassName().replace('.', '/'));
    }

    public static ClassInfo fromCache(String className) {
        return cache.get(className.replace('.', '/'));
    }

    public static ClassInfo fromCache(Type type, TypeLookup lookup) {
        if (type.getSort() == 9) {
            if (lookup == TypeLookup.ELEMENT_TYPE) {
                return ClassInfo.fromCache(type.getElementType(), TypeLookup.ELEMENT_TYPE);
            }
            return OBJECT;
        }
        if (type.getSort() < 9) {
            return null;
        }
        return ClassInfo.fromCache(type.getClassName());
    }

    public static ClassInfo getCommonSuperClass(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return OBJECT;
        }
        return ClassInfo.getCommonSuperClass(ClassInfo.forName(type1), ClassInfo.forName(type2));
    }

    public static ClassInfo getCommonSuperClass(Type type1, Type type2) {
        if (type1 == null || type2 == null || type1.getSort() != 10 || type2.getSort() != 10) {
            return OBJECT;
        }
        return ClassInfo.getCommonSuperClass(ClassInfo.forType(type1, TypeLookup.DECLARED_TYPE), ClassInfo.forType(type2, TypeLookup.DECLARED_TYPE));
    }

    private static ClassInfo getCommonSuperClass(ClassInfo type1, ClassInfo type2) {
        if (type1 == null || type2 == null) {
            return OBJECT;
        }
        return ClassInfo.getCommonSuperClass(type1, type2, false);
    }

    public static ClassInfo getCommonSuperClassOrInterface(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return OBJECT;
        }
        return ClassInfo.getCommonSuperClassOrInterface(ClassInfo.forName(type1), ClassInfo.forName(type2));
    }

    public static ClassInfo getCommonSuperClassOrInterface(Type type1, Type type2) {
        if (type1 == null || type2 == null || type1.getSort() != 10 || type2.getSort() != 10) {
            return OBJECT;
        }
        return ClassInfo.getCommonSuperClassOrInterface(ClassInfo.forType(type1, TypeLookup.DECLARED_TYPE), ClassInfo.forType(type2, TypeLookup.DECLARED_TYPE));
    }

    public static ClassInfo getCommonSuperClassOrInterface(ClassInfo type1, ClassInfo type2) {
        return ClassInfo.getCommonSuperClass(type1, type2, true);
    }

    public static void registerCallback(Callback callback) {
        callbacks.add(callback);
    }

    private static ClassInfo getCommonSuperClass(ClassInfo type1, ClassInfo type2, boolean includeInterfaces) {
        if (type1.hasSuperClass(type2, Traversal.NONE, includeInterfaces)) {
            return type2;
        }
        if (type2.hasSuperClass(type1, Traversal.NONE, includeInterfaces)) {
            return type1;
        }
        if (type1.isInterface() || type2.isInterface()) {
            return OBJECT;
        }
        do {
            if ((type1 = type1.getSuperClass()) != null) continue;
            return OBJECT;
        } while (!type2.hasSuperClass(type1, Traversal.NONE, includeInterfaces));
        return type1;
    }

    static {
        cache.put("java/lang/Object", OBJECT);
    }

    public class Field
    extends Member {
        public Field(Member member) {
            super(member);
        }

        public Field(FieldNode field) {
            this(field, false);
        }

        public Field(FieldNode field, boolean injected) {
            super(Member.Type.FIELD, field.name, field.desc, field.access, injected);
            this.setUnique(Annotations.getVisible(field, Unique.class) != null);
            if (Annotations.getVisible(field, Shadow.class) != null) {
                boolean decoratedFinal = Annotations.getVisible(field, Final.class) != null;
                boolean decoratedMutable = Annotations.getVisible(field, Mutable.class) != null;
                this.setDecoratedFinal(decoratedFinal, decoratedMutable);
            }
        }

        public Field(String name, String desc, int access) {
            super(Member.Type.FIELD, name, desc, access, false);
        }

        public Field(String name, String desc, int access, boolean injected) {
            super(Member.Type.FIELD, name, desc, access, injected);
        }

        @Override
        public ClassInfo getOwner() {
            return ClassInfo.this;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Field)) {
                return false;
            }
            return super.equals(obj);
        }

        @Override
        protected String getDisplayFormat() {
            return "%s:%s";
        }
    }

    public class InterfaceMethod
    extends Method {
        private final ClassInfo owner;

        public InterfaceMethod(Member member) {
            super(member);
            this.owner = member.getOwner();
        }

        @Override
        public ClassInfo getOwner() {
            return this.owner;
        }

        @Override
        public ClassInfo getImplementor() {
            return ClassInfo.this;
        }
    }

    public class Method
    extends Member {
        private final List<FrameData> frames;
        private boolean isAccessor;
        private boolean conformed;

        public Method(Member member) {
            super(member);
            this.frames = member instanceof Method ? ((Method)member).frames : null;
        }

        public Method(MethodNode method) {
            this(method, false);
        }

        public Method(MethodNode method, boolean injected) {
            super(Member.Type.METHOD, method.name, method.desc, method.access, injected);
            this.frames = this.gatherFrames(method);
            this.setUnique(Annotations.getVisible(method, Unique.class) != null);
            this.isAccessor = Annotations.getSingleVisible(method, Accessor.class, Invoker.class) != null;
            boolean decoratedFinal = Annotations.getVisible(method, Final.class) != null;
            boolean decoratedMutable = Annotations.getVisible(method, Mutable.class) != null;
            this.setDecoratedFinal(decoratedFinal, decoratedMutable);
        }

        public Method(String name, String desc) {
            super(Member.Type.METHOD, name, desc, 1, false);
            this.frames = null;
        }

        public Method(String name, String desc, int access) {
            super(Member.Type.METHOD, name, desc, access, false);
            this.frames = null;
        }

        public Method(String name, String desc, int access, boolean injected) {
            super(Member.Type.METHOD, name, desc, access, injected);
            this.frames = null;
        }

        private List<FrameData> gatherFrames(MethodNode method) {
            ArrayList<FrameData> frames = new ArrayList<FrameData>();
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof FrameNode)) continue;
                frames.add(new FrameData(method.instructions.indexOf(insn), (FrameNode)insn, Bytecode.getFirstNonArgLocalIndex(method)));
            }
            return frames;
        }

        public List<FrameData> getFrames() {
            return this.frames;
        }

        @Override
        public ClassInfo getOwner() {
            return ClassInfo.this;
        }

        public boolean isAccessor() {
            return this.isAccessor;
        }

        public boolean isConformed() {
            return this.conformed;
        }

        @Override
        public String renameTo(String name) {
            this.conformed = false;
            return super.renameTo(name);
        }

        public String conform(String name) {
            boolean nameChanged;
            boolean bl = nameChanged = !name.equals(this.getName());
            if (this.conformed && nameChanged) {
                throw new IllegalStateException("Method " + this + " was already conformed. Original= " + this.getOriginalName() + " Current=" + this.getName() + " New=" + name);
            }
            if (nameChanged) {
                this.renameTo(name);
                this.conformed = true;
            }
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Method)) {
                return false;
            }
            return super.equals(obj);
        }
    }

    static abstract class Member {
        private final Type type;
        private final String memberName;
        private final String memberDesc;
        private final boolean isInjected;
        private final int modifiers;
        private String currentName;
        private String currentDesc;
        private boolean decoratedFinal;
        private boolean decoratedMutable;
        private boolean unique;

        protected Member(Member member) {
            this(member.type, member.memberName, member.memberDesc, member.modifiers, member.isInjected);
            this.currentName = member.currentName;
            this.currentDesc = member.currentDesc;
            this.unique = member.unique;
        }

        protected Member(Type type, String name, String desc, int access) {
            this(type, name, desc, access, false);
        }

        protected Member(Type type, String name, String desc, int access, boolean injected) {
            this.type = type;
            this.memberName = name;
            this.memberDesc = desc;
            this.isInjected = injected;
            this.currentName = name;
            this.currentDesc = desc;
            this.modifiers = access;
        }

        public String getOriginalName() {
            return this.memberName;
        }

        public String getName() {
            return this.currentName;
        }

        public String getOriginalDesc() {
            return this.memberDesc;
        }

        public String getDesc() {
            return this.currentDesc;
        }

        public boolean isInjected() {
            return this.isInjected;
        }

        public boolean isRenamed() {
            return !this.currentName.equals(this.memberName);
        }

        public boolean isRemapped() {
            return !this.currentDesc.equals(this.memberDesc);
        }

        public boolean isPrivate() {
            return (this.modifiers & 2) != 0;
        }

        public boolean isStatic() {
            return (this.modifiers & 8) != 0;
        }

        public boolean isAbstract() {
            return (this.modifiers & 0x400) != 0;
        }

        public boolean isFinal() {
            return (this.modifiers & 0x10) != 0;
        }

        public boolean isSynthetic() {
            return (this.modifiers & 0x1000) != 0;
        }

        public boolean isUnique() {
            return this.unique;
        }

        public void setUnique(boolean unique) {
            this.unique = unique;
        }

        public boolean isDecoratedFinal() {
            return this.decoratedFinal;
        }

        public boolean isDecoratedMutable() {
            return this.decoratedMutable;
        }

        protected void setDecoratedFinal(boolean decoratedFinal, boolean decoratedMutable) {
            this.decoratedFinal = decoratedFinal;
            this.decoratedMutable = decoratedMutable;
        }

        public boolean matchesFlags(int flags) {
            return ((~this.modifiers | flags & 2) & 2) != 0 && ((~this.modifiers | flags & 8) & 8) != 0;
        }

        public abstract ClassInfo getOwner();

        public ClassInfo getImplementor() {
            return this.getOwner();
        }

        public int getAccess() {
            return this.modifiers;
        }

        public String renameTo(String name) {
            this.currentName = name;
            return name;
        }

        public String remapTo(String desc) {
            this.currentDesc = desc;
            return desc;
        }

        public boolean equals(String name, String desc) {
            return !(!this.memberName.equals(name) && !this.currentName.equals(name) || !this.memberDesc.equals(desc) && !this.currentDesc.equals(desc));
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Member)) {
                return false;
            }
            Member other = (Member)obj;
            return !(!other.memberName.equals(this.memberName) && !other.currentName.equals(this.currentName) || !other.memberDesc.equals(this.memberDesc) && !other.currentDesc.equals(this.currentDesc));
        }

        public int hashCode() {
            return this.toString().hashCode();
        }

        public String toString() {
            return String.format(this.getDisplayFormat(), this.memberName, this.memberDesc);
        }

        protected String getDisplayFormat() {
            return "%s%s";
        }

        static enum Type {
            METHOD,
            FIELD;

        }
    }

    public static interface Callback {
        public void onInit(ClassInfo var1);
    }

    public static class FrameData {
        private static final String[] FRAMETYPES = new String[]{"NEW", "FULL", "APPEND", "CHOP", "SAME", "SAME1"};
        public final int index;
        public final int type;
        public final int locals;
        public final int size;
        public final int rawSize;

        FrameData(int index, FrameNode frameNode, int initialFrameSize) {
            this.index = index;
            this.type = frameNode.type;
            this.locals = frameNode.local != null ? frameNode.local.size() : 0;
            this.rawSize = Locals.computeFrameSize(frameNode, 0);
            this.size = Math.max(this.rawSize, initialFrameSize);
        }

        public String toString() {
            return String.format("FrameData[index=%d, type=%s, locals=%d size=%d]", this.index, FRAMETYPES[this.type + 1], this.locals, this.size);
        }
    }

    public static enum Traversal {
        NONE(null, false, SearchType.SUPER_CLASSES_ONLY),
        ALL(null, true, SearchType.ALL_CLASSES),
        IMMEDIATE(NONE, true, SearchType.SUPER_CLASSES_ONLY),
        SUPER(ALL, false, SearchType.SUPER_CLASSES_ONLY);

        private final Traversal next;
        private final boolean traverse;
        private final SearchType searchType;

        private Traversal(Traversal next, boolean traverse, SearchType searchType) {
            this.next = next != null ? next : this;
            this.traverse = traverse;
            this.searchType = searchType;
        }

        public Traversal next() {
            return this.next;
        }

        public boolean canTraverse() {
            return this.traverse;
        }

        public SearchType getSearchType() {
            return this.searchType;
        }
    }

    public static enum TypeLookup {
        DECLARED_TYPE,
        ELEMENT_TYPE;

    }

    public static enum SearchType {
        ALL_CLASSES,
        SUPER_CLASSES_ONLY;

    }
}

