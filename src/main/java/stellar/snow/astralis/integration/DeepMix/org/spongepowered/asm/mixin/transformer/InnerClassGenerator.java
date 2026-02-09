package org.spongepowered.asm.mixin.transformer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinCoprocessorNestHost;
import org.spongepowered.asm.mixin.transformer.MixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.include.com.google.common.collect.BiMap;
import org.spongepowered.include.com.google.common.collect.HashBiMap;

final class InnerClassGenerator
implements IClassGenerator {
    private static Class<? extends ClassVisitor> clRemapper;
    private static final ILogger logger;
    private final IConsumer<ISyntheticClassInfo> registry;
    private final Map<String, String> innerClassNames = new HashMap<String, String>();
    private final Map<String, InnerClassInfo> innerClasses = new HashMap<String, InnerClassInfo>();
    private final MixinCoprocessorNestHost nestHostCoprocessor;

    public InnerClassGenerator(IConsumer<ISyntheticClassInfo> registry, MixinCoprocessorNestHost nestHostCoprocessor) {
        this.registry = registry;
        this.nestHostCoprocessor = nestHostCoprocessor;
    }

    @Override
    public String getName() {
        return "inner";
    }

    void registerInnerClass(MixinInfo owner, ClassInfo targetClass, String innerClassName) {
        String coordinate = String.format("%s:%s:%s", owner, innerClassName, targetClass.getName());
        String uniqueName = this.innerClassNames.get(coordinate);
        if (uniqueName != null) {
            return;
        }
        uniqueName = InnerClassGenerator.getUniqueReference(innerClassName, targetClass);
        ClassInfo nestHost = targetClass.resolveNestHost();
        InnerClassInfo info = new InnerClassInfo(owner, targetClass, nestHost, innerClassName, uniqueName, owner);
        this.innerClassNames.put(coordinate, uniqueName);
        this.innerClasses.put(uniqueName, info);
        this.registry.accept(info);
        logger.debug("Inner class {} in {} on {} gets unique name {}", innerClassName, owner.getClassRef(), targetClass, uniqueName);
        this.nestHostCoprocessor.registerNestMember(nestHost.getClassName(), uniqueName);
    }

    BiMap<String, String> getInnerClasses(MixinInfo owner, String targetName) {
        HashBiMap<String, String> innerClasses = HashBiMap.create();
        for (InnerClassInfo innerClass : this.innerClasses.values()) {
            if (innerClass.getMixin() != owner || !targetName.equals(innerClass.getTargetName())) continue;
            innerClasses.put(innerClass.getOriginalName(), innerClass.getName());
        }
        return innerClasses;
    }

    @Override
    public boolean generate(String name, ClassNode classNode) {
        String ref = name.replace('.', '/');
        InnerClassInfo info = this.innerClasses.get(ref);
        if (info == null) {
            return false;
        }
        return this.generate(info, classNode);
    }

    private boolean generate(InnerClassInfo info, ClassNode classNode) {
        try {
            logger.debug("Generating mapped inner class {} (originally {})", info.getName(), info.getOriginalName());
            info.accept(new InnerClassAdapter(InnerClassGenerator.createRemappingAdapter((ClassVisitor)classNode, info), info));
            return true;
        }
        catch (InvalidMixinException ex) {
            throw ex;
        }
        catch (Exception ex) {
            logger.catching(ex);
            return false;
        }
    }

    private static String getUniqueReference(String originalName, ClassInfo targetClass) {
        String name = originalName.substring(originalName.lastIndexOf(36) + 1);
        if (name.matches("^[0-9]+$")) {
            name = "Anonymous";
        }
        return String.format("%s$%s$%s", targetClass, name, UUID.randomUUID().toString().replace("-", ""));
    }

    private static ClassVisitor createRemappingAdapter(ClassVisitor cv, Remapper remapper) throws ReflectiveOperationException {
        if (clRemapper == null) {
            try {
                clRemapper = Class.forName("org.objectweb.asm.commons.ClassRemapper");
            }
            catch (ClassNotFoundException classNotFoundException) {
                // empty catch block
            }
            if (clRemapper == null) {
                try {
                    clRemapper = Class.forName("org.objectweb.asm.commons.RemappingClassAdapter");
                }
                catch (ClassNotFoundException ex) {
                    throw new ClassNotFoundException("org.objectweb.asm.commons.ClassRemapper or org.objectweb.asm.commons.RemappingClassAdapter");
                }
            }
        }
        return clRemapper.getConstructor(ClassVisitor.class, Remapper.class).newInstance(cv, remapper);
    }

    static {
        logger = MixinService.getService().getLogger("mixin");
    }

    static class InnerClassAdapter
    extends ClassVisitor {
        private final InnerClassInfo info;

        InnerClassAdapter(ClassVisitor cv, InnerClassInfo info) {
            super(ASM.API_VERSION, cv);
            this.info = info;
        }

        public void visitNestHost(String nestHost) {
            this.cv.visitNestHost(this.info.getNestHostName());
        }

        public void visitSource(String source, String debug) {
            super.visitSource(source, debug);
            AnnotationVisitor av = this.cv.visitAnnotation("Lorg/spongepowered/asm/mixin/transformer/meta/MixinInner;", false);
            av.visit("mixin", (Object)this.info.getOwner().toString());
            av.visit("name", (Object)this.info.getOriginalName().substring(this.info.getOriginalName().lastIndexOf(47) + 1));
            av.visitEnd();
        }

        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (name.startsWith(this.info.getOriginalName() + "$")) {
                throw new InvalidMixinException((IMixinInfo)this.info.getOwner(), "Found unsupported nested inner class " + name + " in " + this.info.getOriginalName());
            }
            super.visitInnerClass(name, outerName, innerName, access);
        }
    }

    static class InnerClassInfo
    extends Remapper
    implements ISyntheticClassInfo {
        private final IMixinInfo mixin;
        private final ClassInfo targetClassInfo;
        private final String originalName;
        private final String name;
        private final MixinInfo owner;
        private final String ownerName;
        private final String nestHostName;
        private int loadCounter;

        InnerClassInfo(IMixinInfo mixin, ClassInfo targetClass, ClassInfo nestHost, String originalName, String name, MixinInfo owner) {
            this.mixin = mixin;
            this.targetClassInfo = targetClass;
            this.originalName = originalName;
            this.name = name;
            this.owner = owner;
            this.ownerName = owner.getClassRef();
            this.nestHostName = nestHost.getName();
        }

        @Override
        public IMixinInfo getMixin() {
            return this.mixin;
        }

        @Override
        public boolean isLoaded() {
            return this.loadCounter > 0;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getClassName() {
            return this.name.replace('/', '.');
        }

        String getOriginalName() {
            return this.originalName;
        }

        MixinInfo getOwner() {
            return this.owner;
        }

        String getTargetName() {
            return this.targetClassInfo.getName();
        }

        String getNestHostName() {
            return this.nestHostName;
        }

        void accept(ClassVisitor classVisitor) throws ClassNotFoundException, IOException {
            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(this.originalName);
            classNode.accept(classVisitor);
            ++this.loadCounter;
        }

        public String mapFieldName(String owner, String name, String descriptor) {
            ClassInfo.Field field;
            if (this.ownerName.equals(owner) && (field = this.owner.getClassInfo().findField(name, descriptor, 10)) != null) {
                return field.getName();
            }
            return super.mapFieldName(owner, name, descriptor);
        }

        public String mapMethodName(String owner, String name, String desc) {
            ClassInfo.Method method;
            if (this.ownerName.equals(owner) && (method = this.owner.getClassInfo().findMethod(name, desc, 10)) != null) {
                return method.getName();
            }
            return super.mapMethodName(owner, name, desc);
        }

        public String map(String key) {
            if (this.originalName.equals(key)) {
                return this.name;
            }
            if (this.ownerName.equals(key)) {
                return this.targetClassInfo.getName();
            }
            return key;
        }

        public String toString() {
            return this.name;
        }
    }
}

