package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Type;

public class ArrayAccessExpression
extends SimpleExpression {
    public final Expression arr;
    public final Expression index;

    public ArrayAccessExpression(ExpressionSource src, Expression arr, Expression index) {
        super(src);
        this.arr = arr;
        this.index = index;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case 46: 
            case 47: 
            case 48: 
            case 49: 
            case 50: 
            case 51: 
            case 52: 
            case 53: {
                return this.inputsMatch(node, ctx, this.arr, this.index);
            }
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        ctx.decorate(node.getInsn(), "mixinextras_simpleOperationArgs", new Type[]{node.getInput(0).getType(), Type.INT_TYPE});
        ctx.decorate(node.getInsn(), "mixinextras_simpleOperationReturnType", node.getType());
        ctx.decorate(node.getInsn(), "mixinextras_simpleOperationParamNames", new String[]{"array", "index"});
        super.capture(node, ctx);
    }
}

