package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.SimpleExpression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.ArrayCreationInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class ArrayLiteralExpression
extends SimpleExpression {
    public final TypeIdentifier elementType;
    public final List<Expression> values;

    public ArrayLiteralExpression(ExpressionSource src, TypeIdentifier elementType, List<Expression> values) {
        super(src);
        this.elementType = elementType;
        this.values = values;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        ArrayCreationInfo creation = (ArrayCreationInfo)node.getDecoration("mixinextras_persistent_arrayCreationInfo");
        if (creation == null) {
            return false;
        }
        Type newElementType = this.getElementType(node.getInsn());
        if (newElementType == null || !this.elementType.matches(ctx.pool, newElementType)) {
            return false;
        }
        return this.inputsMatch(node, ctx, ctx.allowIncompleteListInputs, this.values.toArray(new Expression[0]));
    }

    private Type getElementType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case 189: {
                return Type.getObjectType((String)((TypeInsnNode)insn).desc);
            }
            case 188: {
                return ExpressionASMUtils.getNewArrayType((IntInsnNode)insn);
            }
        }
        return null;
    }
}

