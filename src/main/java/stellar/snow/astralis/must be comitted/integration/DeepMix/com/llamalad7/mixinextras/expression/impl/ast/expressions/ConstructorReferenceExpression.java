package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Type;

public class ConstructorReferenceExpression
extends SimpleExpression {
    public final TypeIdentifier type;

    public ConstructorReferenceExpression(ExpressionSource src, TypeIdentifier type) {
        super(src);
        this.type = type;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        LMFInfo info = (LMFInfo)node.getDecoration("lmfInfo");
        if (info == null || info.type != LMFInfo.Type.INSTANTIATION) {
            return false;
        }
        return this.type.matches(ctx.pool, Type.getObjectType((String)info.impl.getOwner()));
    }
}

