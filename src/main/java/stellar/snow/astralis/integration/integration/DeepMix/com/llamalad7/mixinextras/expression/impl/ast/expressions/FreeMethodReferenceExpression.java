package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class FreeMethodReferenceExpression
extends SimpleExpression {
    public final MemberIdentifier name;

    public FreeMethodReferenceExpression(ExpressionSource src, MemberIdentifier name) {
        super(src);
        this.name = name;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        LMFInfo info = (LMFInfo)node.getDecoration("lmfInfo");
        if (info == null || info.type != LMFInfo.Type.FREE_METHOD) {
            return false;
        }
        return this.name.matches(ctx.pool, node);
    }
}

