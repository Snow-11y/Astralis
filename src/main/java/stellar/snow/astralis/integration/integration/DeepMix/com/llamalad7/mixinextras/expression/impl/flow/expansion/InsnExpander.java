package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.ExpressionService;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

public abstract class InsnExpander
implements FlowPostProcessor {
    private static final String INSN_COMPONENT = "expandedInsnComponent";
    private static final String EXPANSION = "expansion";
    private final Map<AbstractInsnNode, Expansion> expansions = new IdentityHashMap<AbstractInsnNode, Expansion>();

    @Override
    public abstract void process(FlowValue var1, FlowPostProcessor.OutputSink var2);

    public abstract void expand(Target var1, InjectionNodes.InjectionNode var2, Expansion var3);

    protected final void registerComponent(FlowValue node, InsnComponent component, AbstractInsnNode compound) {
        node.decorate(INSN_COMPONENT, component);
        this.expansions.computeIfAbsent(compound, x$0 -> new Expansion((AbstractInsnNode)x$0));
        node.decorate(EXPANSION, this.expansions.get(compound));
    }

    protected final void expandInsn(Target target, InjectionNodes.InjectionNode node, AbstractInsnNode ... insns) {
        InsnList insnList = new InsnList();
        for (AbstractInsnNode insn : insns) {
            insnList.add(insn);
        }
        AbstractInsnNode insn = node.getCurrentTarget();
        target.insns.insert(insn, insnList);
        target.replaceNode(insn, (AbstractInsnNode)InsnExpander.dummyInsn());
    }

    protected static InsnNode dummyInsn() {
        return new InsnNode(0);
    }

    public static Expansion prepareExpansion(FlowValue node, Target target, InjectionInfo info, ExpressionContext ctx) {
        if (!InsnExpander.hasExpansion(node)) {
            return null;
        }
        InsnExpander.checkSupportsExpansion(info, ctx.type);
        Expansion expansion = (Expansion)node.getDecoration(EXPANSION);
        AbstractInsnNode compoundInsn = expansion.compound;
        InjectionNodes.InjectionNode compoundNode = target.addInjectionNode(compoundInsn);
        if (!compoundNode.hasDecoration("mixinextras_expansionInfo")) {
            compoundNode.decorate("mixinextras_expansionInfo", expansion);
        }
        expansion.registerInterest(info, (InsnComponent)node.getDecoration(INSN_COMPONENT));
        return expansion;
    }

    public static InjectionNodes.InjectionNode doExpansion(InjectionNodes.InjectionNode node, Target target, InjectionInfo info) {
        Expansion expansion = (Expansion)node.getDecoration("mixinextras_expansionInfo");
        if (expansion == null) {
            return node;
        }
        expansion.doExpansion(target, node);
        return target.addInjectionNode(expansion.getTargetInsn(info));
    }

    private static void checkSupportsExpansion(InjectionInfo info, ExpressionContext.Type type) {
        switch (type) {
            case SLICE: 
            case INJECT: 
            case MODIFY_VARIABLE: {
                return;
            }
            case MODIFY_EXPRESSION_VALUE: 
            case WRAP_OPERATION: {
                return;
            }
        }
        throw ExpressionService.getInstance().makeInvalidInjectionException(info, String.format("Expression context type %s does not support compound instructions!", new Object[]{type}));
    }

    public static AbstractInsnNode getRepresentative(FlowValue node) {
        Expansion expansion = (Expansion)node.getDecoration(EXPANSION);
        if (expansion != null) {
            return expansion.compound;
        }
        return node.getInsn();
    }

    public static boolean hasExpansion(FlowValue node) {
        return node.hasDecoration(EXPANSION);
    }

    public static void addExpansionStep(FlowValue node, Consumer<InjectionNodes.InjectionNode> step) {
        Expansion expansion = (Expansion)node.getDecoration(EXPANSION);
        expansion.addExpansionStep((InsnComponent)node.getDecoration(INSN_COMPONENT), step);
    }

    public class Expansion {
        private final Map<InjectionInfo, InsnComponent> interests = new IdentityHashMap<InjectionInfo, InsnComponent>();
        private final Map<InsnComponent, List<Consumer<InjectionNodes.InjectionNode>>> expansionSteps = new HashMap<InsnComponent, List<Consumer<InjectionNodes.InjectionNode>>>();
        private final Map<InsnComponent, AbstractInsnNode> expandedInsns = new HashMap<InsnComponent, AbstractInsnNode>();
        private boolean expanded = false;
        public final AbstractInsnNode compound;

        public Expansion(AbstractInsnNode compound) {
            this.compound = compound;
        }

        public void registerInterest(InjectionInfo info, InsnComponent component) {
            if (this.interests.put(info, component) != null) {
                throw new UnsupportedOperationException("The same injector should not target multiple parts of a compound instruction!");
            }
        }

        public void decorate(InjectionInfo info, String key, Object value) {
            this.addExpansionStep(this.interests.get(info), node -> node.decorate(key, value));
        }

        public void decorateInjectorSpecific(InjectionInfo info, String key, Object value) {
            this.addExpansionStep(this.interests.get(info), node -> ExpressionService.getInstance().decorateInjectorSpecific((InjectionNodes.InjectionNode)node, info, key, value));
        }

        public Set<InsnComponent> registeredInterests() {
            return new HashSet<InsnComponent>(this.interests.values());
        }

        void addExpansionStep(InsnComponent component, Consumer<InjectionNodes.InjectionNode> step) {
            this.expansionSteps.computeIfAbsent(component, k -> new ArrayList()).add(step);
        }

        void doExpansion(Target target, InjectionNodes.InjectionNode node) {
            if (this.expanded) {
                return;
            }
            this.expanded = true;
            InsnExpander.this.expand(target, node, this);
            for (Map.Entry<InsnComponent, List<Consumer<InjectionNodes.InjectionNode>>> steps : this.expansionSteps.entrySet()) {
                InjectionNodes.InjectionNode newNode = target.addInjectionNode(this.expandedInsns.get(steps.getKey()));
                for (Consumer<InjectionNodes.InjectionNode> step : steps.getValue()) {
                    step.accept(newNode);
                }
            }
            this.expansionSteps.clear();
        }

        AbstractInsnNode getTargetInsn(InjectionInfo info) {
            return this.expandedInsns.get(this.interests.get(info));
        }

        protected AbstractInsnNode registerInsn(InsnComponent component, AbstractInsnNode insn) {
            this.expandedInsns.put(component, insn);
            return insn;
        }
    }

    public static interface InsnComponent {
    }
}

