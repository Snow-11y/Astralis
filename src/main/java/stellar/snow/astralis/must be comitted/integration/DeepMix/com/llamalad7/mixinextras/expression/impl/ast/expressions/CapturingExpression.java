package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class CapturingExpression
extends SimpleExpression {
    public final Expression expression;

    public CapturingExpression(ExpressionSource src, Expression expression) {
        super(src);
        this.expression = expression;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        boolean matches = this.expression.matches(node, ctx);
        if (matches) {
            this.expression.capture(node, ctx);
        }
        return matches;
    }
}

