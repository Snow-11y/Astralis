package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.expression.impl.ExpressionParserFacade;
import com.llamalad7.mixinextras.expression.impl.ExpressionService;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.ComplexDataException;
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.point.RuntimeExpressionService;
import com.llamalad7.mixinextras.expression.impl.pool.BytecodeIdentifierPool;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import com.llamalad7.mixinextras.utils.MixinConfigUtils;
import com.llamalad7.mixinextras.utils.TargetDecorations;
import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.CallbackInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.ModifyArgInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.ModifyArgsInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.ModifyConstantInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.ModifyVariableInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.RedirectInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

@InjectionPoint.AtCode(value="MIXINEXTRAS:EXPRESSION")
public class ExpressionInjectionPoint
extends InjectionPoint {
    private static List<Target> CURRENT_TARGETS;
    private static InjectionInfo CURRENT_INFO;
    private final int ordinal;
    private final String id;
    private final boolean isInSlice;
    private boolean initialized;
    private IdentifierPool pool;
    private List<com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression> expressions;
    private ExpressionContext.Type contextType;

    public ExpressionInjectionPoint(InjectionPointData data) {
        super(data);
        this.ordinal = data.getOrdinal();
        this.id = data.getId() != null ? data.getId() : "";
        this.isInSlice = data.get("mixinextras_isInSlice", false);
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        if (insns.size() == 0) {
            return false;
        }
        final Target target = this.getTarget(insns);
        if (!this.initialized) {
            this.initialize(target);
        }
        Collection flows = TargetDecorations.getOrPut(target, "ValueFlow", () -> FlowInterpreter.analyze(CURRENT_INFO.getClassNode(), target.method, null));
        HashSet result = new HashSet();
        final IdentityHashMap genericDecorations = new IdentityHashMap();
        final IdentityHashMap injectorSpecificDecorations = new IdentityHashMap();
        final ArrayList captured = new ArrayList();
        Expression.OutputSink sink = new Expression.OutputSink(){

            @Override
            public void capture(FlowValue node, com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression expr, ExpressionContext ctx) {
                Map injectorSpecific;
                BiConsumer<String, Object> decorateInjectorSpecific;
                BiConsumer<String, Object> decorate;
                AbstractInsnNode targetInsn;
                AbstractInsnNode capturedInsn = node.getInsn();
                InsnExpander.Expansion expansion = InsnExpander.prepareExpansion(node, target, CURRENT_INFO, ctx);
                if (expansion != null) {
                    targetInsn = expansion.compound;
                    decorate = (k, v) -> expansion.decorate(CURRENT_INFO, (String)k, v);
                    decorateInjectorSpecific = (k, v) -> expansion.decorateInjectorSpecific(CURRENT_INFO, (String)k, v);
                } else {
                    targetInsn = node.getInsn();
                    InjectionNodes.InjectionNode injectionNode = target.addInjectionNode(capturedInsn);
                    decorate = injectionNode::decorate;
                    decorateInjectorSpecific = (k, v) -> InjectorUtils.decorateInjectorSpecific(injectionNode, CURRENT_INFO, k, v);
                }
                Map decorations = (Map)genericDecorations.get(capturedInsn);
                if (decorations != null) {
                    for (Map.Entry decoration : decorations.entrySet()) {
                        decorate.accept((String)decoration.getKey(), decoration.getValue());
                    }
                }
                if ((injectorSpecific = (Map)injectorSpecificDecorations.get(capturedInsn)) != null) {
                    for (Map.Entry<Object, Object> entry : injectorSpecific.entrySet()) {
                        decorateInjectorSpecific.accept((String)entry.getKey(), entry.getValue());
                    }
                }
                for (Map.Entry<Object, Object> entry : node.getDecorations().entrySet()) {
                    if (!((String)entry.getKey()).startsWith("mixinextras_persistent_")) continue;
                    decorate.accept((String)entry.getKey(), entry.getValue());
                }
                captured.add(targetInsn);
            }

            @Override
            public void decorate(AbstractInsnNode insn, String key, Object value) {
                genericDecorations.computeIfAbsent(insn, k -> new HashMap()).put(key, value);
            }

            @Override
            public void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value) {
                injectorSpecificDecorations.computeIfAbsent(insn, k -> new HashMap()).put(key, value);
            }
        };
        ExpressionContext ctx = new ExpressionContext(this.pool, sink, target.classNode, target.method, this.contextType, false);
        for (com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression expr : this.expressions) {
            for (FlowValue flow : flows) {
                try {
                    if (expr.matches(flow, ctx)) {
                        result.addAll(captured);
                    }
                }
                catch (ComplexDataException complexDataException) {
                    // empty catch block
                }
                genericDecorations.clear();
                injectorSpecificDecorations.clear();
                captured.clear();
            }
        }
        int i = 0;
        boolean found = false;
        for (AbstractInsnNode insn : insns) {
            if (!result.contains(insn)) continue;
            if (this.ordinal < 0 || this.ordinal == i) {
                nodes.add(insn);
                found = true;
            }
            ++i;
        }
        return found;
    }

    private void initialize(Target target) {
        this.checkDeclaredMinVersion();
        this.initialized = true;
        AnnotationNode poolAnnotation = ASMUtils.getRepeatedMEAnnotation(CURRENT_INFO.getMethod(), Definition.class);
        this.pool = new BytecodeIdentifierPool(target, CURRENT_INFO, poolAnnotation);
        this.expressions = this.parseExpressions();
        this.contextType = this.selectContextType();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void withContext(InjectionInfo info, Runnable runnable) {
        InjectionInfo oldInfo = CURRENT_INFO;
        List<Target> oldTargets = CURRENT_TARGETS;
        try {
            CURRENT_INFO = info;
            CURRENT_TARGETS = CompatibilityHelper.getTargets(info);
            runnable.run();
        }
        finally {
            CURRENT_INFO = oldInfo;
            CURRENT_TARGETS = oldTargets;
        }
    }

    private Target getTarget(InsnList insns) {
        AbstractInsnNode marker = insns.getFirst();
        Target target = null;
        for (Target candidate : CURRENT_TARGETS) {
            if (!candidate.method.instructions.contains(marker)) continue;
            target = candidate;
            break;
        }
        if (target == null) {
            throw new IllegalStateException("Could not find target for " + insns);
        }
        CURRENT_TARGETS.remove(target);
        CURRENT_TARGETS.add(target);
        return target;
    }

    private void checkDeclaredMinVersion() {
        IMixinConfig config = CompatibilityHelper.getMixin(CURRENT_INFO).getMixin().getConfig();
        MixinConfigUtils.requireMinVersion(config, MixinExtrasVersion.V0_5_0_BETA_1, "@Expression");
    }

    private List<com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression> parseExpressions() {
        List<String> strings = this.getMatchingExpressions(CURRENT_INFO.getMethod());
        return strings.stream().map(ExpressionParserFacade::parse).collect(Collectors.toList());
    }

    private List<String> getMatchingExpressions(MethodNode method) {
        ArrayList<String> result = new ArrayList<String>();
        AnnotationNode expressions = ASMUtils.getRepeatedMEAnnotation(method, Expression.class);
        for (AnnotationNode expression : Annotations.getValue(expressions, "value", true)) {
            if (!Annotations.getValue(expression, "id", "").equals(this.id)) continue;
            result.addAll(Annotations.getValue(expression, "value", true));
        }
        if (result.isEmpty()) {
            String idText = this.id.isEmpty() ? "" : "for id '" + this.id + "' ";
            throw new IllegalStateException("No expression found " + idText + "on " + CURRENT_INFO);
        }
        return result;
    }

    private ExpressionContext.Type selectContextType() {
        if (this.isInSlice) {
            return ExpressionContext.Type.SLICE;
        }
        InjectionInfo info = CURRENT_INFO;
        while (info instanceof WrapperInjectionInfo) {
            info = ((WrapperInjectionInfo)info).getDelegate();
        }
        if (info instanceof CallbackInjectionInfo) {
            return ExpressionContext.Type.INJECT;
        }
        if (info instanceof ModifyArgInjectionInfo) {
            return ExpressionContext.Type.MODIFY_ARG;
        }
        if (info instanceof ModifyArgsInjectionInfo) {
            return ExpressionContext.Type.MODIFY_ARGS;
        }
        if (info instanceof ModifyConstantInjectionInfo) {
            return ExpressionContext.Type.MODIFY_CONSTANT;
        }
        if (info instanceof ModifyExpressionValueInjectionInfo) {
            return ExpressionContext.Type.MODIFY_EXPRESSION_VALUE;
        }
        if (info instanceof ModifyReceiverInjectionInfo) {
            return ExpressionContext.Type.MODIFY_RECEIVER;
        }
        if (info instanceof ModifyReturnValueInjectionInfo) {
            return ExpressionContext.Type.MODIFY_RETURN_VALUE;
        }
        if (info instanceof ModifyVariableInjectionInfo) {
            return ExpressionContext.Type.MODIFY_VARIABLE;
        }
        if (info instanceof RedirectInjectionInfo) {
            return ExpressionContext.Type.REDIRECT;
        }
        if (info instanceof WrapOperationInjectionInfo) {
            return ExpressionContext.Type.WRAP_OPERATION;
        }
        if (info instanceof WrapWithConditionInjectionInfo) {
            return ExpressionContext.Type.WRAP_WITH_CONDITION;
        }
        return ExpressionContext.Type.CUSTOM;
    }

    static {
        ExpressionService.offerInstance(new RuntimeExpressionService());
    }
}

