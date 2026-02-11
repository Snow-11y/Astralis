package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

public class IdentifierAssignmentExpression
extends Expression {
    public final MemberIdentifier identifier;
    public final Expression value;

    public IdentifierAssignmentExpression(ExpressionSource src, MemberIdentifier identifier, Expression value) {
        super(src);
        this.identifier = identifier;
        this.value = value;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case 54: 
            case 55: 
            case 56: 
            case 57: 
            case 58: 
            case 179: {
                return this.identifier.matches(ctx.pool, node) && this.inputsMatch(node, ctx, this.value);
            }
        }
        return false;
    }
}

