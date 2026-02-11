package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.BinaryExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.IntLiteralExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class UnaryExpression
extends SimpleExpression {
    public final Operator operator;
    public final Expression expression;

    public UnaryExpression(ExpressionSource src, Operator operator, Expression expression) {
        super(src);
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        switch (this.operator) {
            case MINUS: {
                switch (node.getInsn().getOpcode()) {
                    case 116: 
                    case 117: 
                    case 118: 
                    case 119: {
                        return this.inputsMatch(node, ctx, this.expression);
                    }
                }
            }
            case BITWISE_NOT: {
                return new BinaryExpression(this.src, this.expression, BinaryExpression.Operator.BITWISE_XOR, new IntLiteralExpression(null, -1L)).matchesImpl(node, ctx);
            }
        }
        return false;
    }

    public static enum Operator {
        MINUS,
        BITWISE_NOT;

    }
}

