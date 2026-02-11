package org.spongepowered.asm.transformers;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.transformers.MixinClassReader;
import org.spongepowered.asm.transformers.MixinClassWriter;

public abstract class TreeTransformer
implements ILegacyClassTransformer {
    private ClassReader classReader;
    private ClassNode classNode;

    protected final ClassNode readClass(String className, byte[] basicClass) {
        return this.readClass(className, basicClass, true);
    }

    protected final ClassNode readClass(String className, byte[] basicClass, boolean cacheReader) {
        MixinClassReader classReader = new MixinClassReader(basicClass, className);
        if (cacheReader) {
            this.classReader = classReader;
        }
        ClassNode classNode = new ClassNode();
        classReader.accept((ClassVisitor)classNode, 8);
        return classNode;
    }

    protected final byte[] writeClass(ClassNode classNode) {
        if (this.classReader != null && this.classNode == classNode) {
            this.classNode = null;
            MixinClassWriter writer = new MixinClassWriter(this.classReader, 3);
            this.classReader = null;
            classNode.accept((ClassVisitor)writer);
            return writer.toByteArray();
        }
        this.classNode = null;
        MixinClassWriter writer = new MixinClassWriter(3);
        classNode.accept((ClassVisitor)writer);
        return writer.toByteArray();
    }
}

