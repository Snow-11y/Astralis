package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;

public interface MemberIdentifier {
    public boolean matches(IdentifierPool var1, FlowValue var2);
}

