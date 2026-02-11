package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class IdentifierExpression
extends SimpleExpression {
    public final String identifier;

    public IdentifierExpression(ExpressionSource src, String identifier) {
        super(src);
        this.identifier = identifier;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case 21: 
            case 22: 
            case 23: 
            case 24: 
            case 25: 
            case 178: {
                return ctx.pool.matchesMember(this.identifier, node);
            }
        }
        return false;
    }
}

