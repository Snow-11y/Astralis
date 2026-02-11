package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;

public class ArrayStoreExpression
extends Expression {
    public final Expression arr;
    public final Expression index;
    public final Expression value;

    public ArrayStoreExpression(ExpressionSource src, Expression arr, Expression index, Expression value) {
        super(src);
        this.arr = arr;
        this.index = index;
        this.value = value;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case 79: 
            case 80: 
            case 81: 
            case 82: 
            case 83: 
            case 84: 
            case 85: 
            case 86: {
                return this.inputsMatch(node, ctx, this.arr, this.index, this.value);
            }
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        Type arrayType = node.getInput(0).getType();
        ctx.decorate(node.getInsn(), "mixinextras_simpleOperationArgs", new Type[]{arrayType, Type.INT_TYPE, ExpressionASMUtils.getInnerType(arrayType)});
        ctx.decorate(node.getInsn(), "mixinextras_simpleOperationReturnType", Type.VOID_TYPE);
        ctx.decorate(node.getInsn(), "mixinextras_simpleOperationParamNames", new String[]{"array", "index", "value"});
        super.capture(node, ctx);
    }
}

