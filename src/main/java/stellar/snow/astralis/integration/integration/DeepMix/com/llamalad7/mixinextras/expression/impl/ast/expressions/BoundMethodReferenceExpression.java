package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class BoundMethodReferenceExpression
extends SimpleExpression {
    public final Expression receiver;
    public final MemberIdentifier name;

    public BoundMethodReferenceExpression(ExpressionSource src, Expression receiver, MemberIdentifier name) {
        super(src);
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        LMFInfo info = (LMFInfo)node.getDecoration("lmfInfo");
        if (info == null || info.type != LMFInfo.Type.BOUND_METHOD || !this.name.matches(ctx.pool, node)) {
            return false;
        }
        ctx.reportPartialMatch(node, this);
        return this.receiver.matches(node.getInput(0), ctx);
    }
}

