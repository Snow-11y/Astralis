package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ExpressionContext {
    public final IdentifierPool pool;
    private final Expression.OutputSink sink;
    public final ClassNode classNode;
    public final MethodNode method;
    public final Type type;
    public final boolean isStatic;
    public final boolean allowIncompleteListInputs;

    public ExpressionContext(IdentifierPool pool, Expression.OutputSink sink, ClassNode classNode, MethodNode method, Type type, boolean allowIncompleteListInputs) {
        this.pool = pool;
        this.sink = sink;
        this.classNode = classNode;
        this.method = method;
        this.type = type;
        this.isStatic = (method.access & 8) != 0;
        this.allowIncompleteListInputs = allowIncompleteListInputs;
    }

    public void capture(FlowValue node, Expression expression) {
        this.sink.capture(node, expression, this);
    }

    public void decorate(AbstractInsnNode insn, String key, Object value) {
        this.sink.decorate(insn, key, value);
    }

    public void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value) {
        this.sink.decorateInjectorSpecific(insn, key, value);
    }

    public void reportMatchStatus(FlowValue node, Expression expr, boolean matched) {
        this.sink.reportMatchStatus(node, expr, matched);
    }

    public void reportPartialMatch(FlowValue node, Expression expr) {
        this.sink.reportPartialMatch(node, expr);
    }

    public static enum Type {
        CUSTOM,
        INJECT,
        MODIFY_ARG,
        MODIFY_ARGS,
        MODIFY_CONSTANT,
        MODIFY_EXPRESSION_VALUE,
        MODIFY_RECEIVER,
        MODIFY_RETURN_VALUE,
        MODIFY_VARIABLE,
        REDIRECT,
        SLICE,
        WRAP_OPERATION,
        WRAP_WITH_CONDITION;

    }
}

