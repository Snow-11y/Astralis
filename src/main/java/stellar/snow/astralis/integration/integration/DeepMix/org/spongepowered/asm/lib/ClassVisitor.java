package org.spongepowered.asm.lib;

import org.spongepowered.asm.lib.AnnotationVisitor;
import org.spongepowered.asm.lib.Attribute;
import org.spongepowered.asm.lib.FieldVisitor;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.TypePath;
import org.spongepowered.asm.mixin.throwables.CompanionPluginError;

public abstract class ClassVisitor {
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        throw new CompanionPluginError("ClassVisitor.visit");
    }

    public void visitSource(String source, String debug) {
        throw new CompanionPluginError("ClassVisitor.visitSource");
    }

    public void visitOuterClass(String owner, String name, String desc) {
        throw new CompanionPluginError("ClassVisitor.visitOuterClass");
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        throw new CompanionPluginError("ClassVisitor.visitAnnotation");
    }

    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        throw new CompanionPluginError("ClassVisitor.visitTypeAnnotation");
    }

    public void visitAttribute(Attribute attr) {
        throw new CompanionPluginError("ClassVisitor.visitAttribute");
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        throw new CompanionPluginError("ClassVisitor.visitInnerClass");
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        throw new CompanionPluginError("ClassVisitor.visitField");
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        throw new CompanionPluginError("ClassVisitor.visitMethod");
    }

    public void visitEnd() {
        throw new CompanionPluginError("ClassVisitor.visitEnd");
    }
}

