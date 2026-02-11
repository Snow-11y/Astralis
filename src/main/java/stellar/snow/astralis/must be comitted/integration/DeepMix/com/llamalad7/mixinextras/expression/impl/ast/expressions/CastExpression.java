package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;

public class CastExpression
extends SimpleExpression {
    public final TypeIdentifier type;
    public final Expression expression;

    public CastExpression(ExpressionSource src, TypeIdentifier type, Expression expression) {
        super(src);
        this.type = type;
        this.expression = expression;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        Type castType = ExpressionASMUtils.getCastType(node.getInsn());
        return castType != null && this.type.matches(ctx.pool, castType) && this.inputsMatch(node, ctx, this.expression);
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        if (node.getInsn().getOpcode() == 192) {
            ctx.decorate(node.getInsn(), "mixinextras_simpleOperationArgs", new Type[]{ExpressionASMUtils.OBJECT_TYPE});
            ctx.decorate(node.getInsn(), "mixinextras_simpleOperationReturnType", node.getType());
            ctx.decorate(node.getInsn(), "mixinextras_simpleOperationParamNames", new String[]{"object"});
        }
        super.capture(node, ctx);
    }
}

