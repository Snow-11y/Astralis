package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MemberAccessExpression
extends SimpleExpression {
    public final Expression receiver;
    public final MemberIdentifier name;

    public MemberAccessExpression(ExpressionSource src, Expression receiver, MemberIdentifier name) {
        super(src);
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        switch (insn.getOpcode()) {
            case 180: 
            case 190: {
                return this.name.matches(ctx.pool, node) && this.inputsMatch(node, ctx, this.receiver);
            }
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        if (node.getInsn().getOpcode() == 190) {
            ctx.decorate(node.getInsn(), "mixinextras_simpleOperationArgs", new Type[]{node.getInput(0).getType()});
            ctx.decorate(node.getInsn(), "mixinextras_simpleOperationReturnType", Type.INT_TYPE);
            ctx.decorate(node.getInsn(), "mixinextras_simpleOperationParamNames", new String[]{"array", "index"});
        }
        super.capture(node, ctx);
    }
}

