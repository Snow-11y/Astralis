package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Type;

public interface TypeIdentifier {
    public boolean matches(IdentifierPool var1, Type var2);
}

