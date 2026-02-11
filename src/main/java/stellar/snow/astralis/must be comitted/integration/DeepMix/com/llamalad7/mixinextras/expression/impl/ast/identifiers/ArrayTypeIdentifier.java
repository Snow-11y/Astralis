package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Type;

public class ArrayTypeIdentifier
implements TypeIdentifier {
    public final int dims;
    public final TypeIdentifier elementType;

    public ArrayTypeIdentifier(int dims, TypeIdentifier elementType) {
        this.dims = dims;
        this.elementType = elementType;
    }

    @Override
    public boolean matches(IdentifierPool pool, Type type) {
        return type.getSort() == 9 && type.getDimensions() >= this.dims && this.elementType.matches(pool, Type.getType((String)type.getDescriptor().substring(this.dims)));
    }
}

