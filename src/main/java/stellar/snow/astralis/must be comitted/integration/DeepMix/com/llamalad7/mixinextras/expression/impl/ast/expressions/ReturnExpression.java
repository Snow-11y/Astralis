package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class ReturnExpression
extends Expression {
    public final Expression value;

    public ReturnExpression(ExpressionSource src, Expression value) {
        super(src);
        this.value = value;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case 172: 
            case 173: 
            case 174: 
            case 175: 
            case 176: {
                return this.inputsMatch(node, ctx, this.value);
            }
        }
        return false;
    }
}

