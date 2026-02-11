package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.pool.ExactTypeDef;
import com.llamalad7.mixinextras.expression.impl.pool.FieldDef;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.pool.LocalDef;
import com.llamalad7.mixinextras.expression.impl.pool.MethodDef;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

public class BytecodeIdentifierPool
extends IdentifierPool {
    public BytecodeIdentifierPool(Target target, InjectionInfo info, AnnotationNode poolAnnotation) {
        for (AnnotationNode entry : Annotations.getValue(poolAnnotation, "value", true)) {
            this.parseEntry(entry, target, info);
        }
    }

    private void parseEntry(AnnotationNode entry, Target target, InjectionInfo info) {
        String id = (String)Annotations.getValue(entry, "id");
        for (String method : Annotations.getValue(entry, "method", true)) {
            this.addMember(id, new MethodDef(method, info));
        }
        for (String method : Annotations.getValue(entry, "field", true)) {
            this.addMember(id, new FieldDef(method, info));
        }
        for (Type type : Annotations.getValue(entry, "type", true)) {
            this.addType(id, new ExactTypeDef(type));
        }
        for (AnnotationNode local : Annotations.getValue(entry, "local", true)) {
            this.addMember(id, new LocalDef(local, info, target));
        }
    }
}

