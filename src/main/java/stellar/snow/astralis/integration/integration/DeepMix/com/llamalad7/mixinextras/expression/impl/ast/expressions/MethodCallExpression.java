package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.MethodCallType;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import java.util.List;

public class MethodCallExpression
extends SimpleExpression {
    public final Expression receiver;
    public final MemberIdentifier name;
    public final List<Expression> arguments;

    public MethodCallExpression(ExpressionSource src, Expression receiver, MemberIdentifier name, List<Expression> arguments) {
        super(src);
        this.receiver = receiver;
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        if (!MethodCallType.NORMAL.matches(node)) {
            return false;
        }
        if (!this.name.matches(ctx.pool, node)) {
            return false;
        }
        Expression[] inputs = ArrayUtils.add(this.arguments.toArray(new Expression[0]), 0, this.receiver);
        return this.inputsMatch(node, ctx, ctx.allowIncompleteListInputs, inputs);
    }
}

