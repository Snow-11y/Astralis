package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.CapturingExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.WildcardExpression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.StringConcatInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionUtil;
import org.objectweb.asm.tree.AbstractInsnNode;

public class BinaryExpression
extends SimpleExpression {
    public final Expression left;
    public final Operator operator;
    public final Expression right;

    public BinaryExpression(ExpressionSource src, Expression left, Operator operator, Expression right) {
        super(src);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        if (this.operator.matches(node.getInsn()) && this.inputsMatch(node, ctx, this.left, this.right)) {
            return true;
        }
        StringConcatInfo concat = (StringConcatInfo)node.getDecoration("stringConcatInfo");
        if (this.operator != Operator.PLUS || concat == null) {
            return false;
        }
        ctx.reportPartialMatch(node, this);
        if (node == concat.toStringCall) {
            node = node.getInput(0);
        }
        if (!this.right.matches(node.getInput(1), ctx)) {
            return false;
        }
        if (concat.isFirstConcat) {
            return this.left.matches(concat.initialComponent, ctx);
        }
        Expression innerLeft = ExpressionUtil.skipCapturesDown(this.left);
        if (innerLeft instanceof WildcardExpression) {
            if (this.left instanceof CapturingExpression) {
                this.checkSupportsStringConcat(ctx.type);
                ctx.decorateInjectorSpecific(node.getInput(0).getInsn(), "mixinextras_isStringConcatExpression", true);
            }
            return this.left.matches(node.getInput(0), ctx);
        }
        if (innerLeft instanceof BinaryExpression && ((BinaryExpression)innerLeft).operator == Operator.PLUS) {
            return this.left.matches(node.getInput(0), ctx);
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        StringConcatInfo concat = (StringConcatInfo)node.getDecoration("stringConcatInfo");
        if (concat == null) {
            super.capture(node, ctx);
            return;
        }
        this.checkSupportsStringConcat(ctx.type);
        if (concat.isBuilder) {
            ctx.decorateInjectorSpecific(node.getInsn(), "mixinextras_isStringConcatExpression", true);
        }
        super.capture(node, ctx);
    }

    private void checkSupportsStringConcat(ExpressionContext.Type type) {
        switch (type) {
            case SLICE: 
            case INJECT: 
            case MODIFY_VARIABLE: {
                return;
            }
            case MODIFY_EXPRESSION_VALUE: {
                return;
            }
        }
        throw new UnsupportedOperationException(String.format("Expression context type %s does not support string concat!", new Object[]{type}));
    }

    public static enum Operator {
        MULT(104, 105, 106, 107),
        DIV(108, 109, 110, 111),
        MOD(112, 113, 114, 115),
        PLUS(96, 97, 98, 99),
        MINUS(100, 101, 102, 103),
        SHL(120, 121),
        SHR(122, 123),
        USHR(124, 125),
        BITWISE_AND(126, 127),
        BITWISE_XOR(130, 131),
        BITWISE_OR(128, 129);

        private final int[] opcodes;

        private Operator(int ... opcodes) {
            this.opcodes = opcodes;
        }

        public boolean matches(AbstractInsnNode insn) {
            for (int opcode : this.opcodes) {
                if (opcode != insn.getOpcode()) continue;
                return true;
            }
            return false;
        }
    }
}

