package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.expression.impl.flow.Boxing;
import com.llamalad7.mixinextras.expression.impl.flow.ComputedFlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.FlowContext;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.LocalsCalculator;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.IincExpander;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.StringConcatFactoryExpander;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.UnaryComparisonExpander;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.CallTaggingPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.InstantiationPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.NewArrayPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.SplitNodeRemovalPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.StringConcatPostProcessor;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.spongepowered.asm.util.asm.ASM;

public class FlowInterpreter
extends Interpreter<FlowValue> {
    private final FlowContext context;
    private final Map<AbstractInsnNode, FlowValue> cache = new IdentityHashMap<AbstractInsnNode, FlowValue>();
    private final Map<VarInsnNode, Type> localTypes;
    private final List<FlowPostProcessor> postProcessors;

    protected FlowInterpreter(ClassNode classNode, MethodNode methodNode, FlowContext ctx) {
        super(ASM.API_VERSION);
        this.context = ctx;
        this.localTypes = LocalsCalculator.getLocalTypes(classNode, methodNode, ctx);
        this.postProcessors = Arrays.asList(new NewArrayPostProcessor(methodNode), new IincExpander(), new UnaryComparisonExpander(), new StringConcatFactoryExpander(), new InstantiationPostProcessor(), new StringConcatPostProcessor(), new CallTaggingPostProcessor(classNode, methodNode), new LMFPostProcessor(classNode), new SplitNodeRemovalPostProcessor());
    }

    public static Collection<FlowValue> analyze(ClassNode classNode, MethodNode methodNode, FlowContext ctx) {
        FlowInterpreter interpreter = new FlowInterpreter(classNode, methodNode, ctx);
        try {
            new Analyzer((Interpreter)interpreter).analyze(classNode.name, methodNode);
        }
        catch (AnalyzerException e) {
            throw new RuntimeException("Failed to analyze value flow: ", e);
        }
        return new ArrayList<FlowValue>(interpreter.finish());
    }

    public Collection<FlowValue> finish() {
        Set<FlowValue> flows = Collections.newSetFromMap(new IdentityHashMap());
        flows.addAll(this.cache.values());
        for (FlowValue value : flows) {
            value.finish();
        }
        for (FlowValue value : flows) {
            value.onFinished();
        }
        for (FlowPostProcessor postProcessor : this.postProcessors) {
            final Set<FlowValue> synthetic = Collections.newSetFromMap(new IdentityHashMap());
            final ArrayList newFlows = new ArrayList();
            FlowPostProcessor.OutputSink sink = new FlowPostProcessor.OutputSink(){

                @Override
                public void markAsSynthetic(FlowValue node) {
                    if (!node.isComplex()) {
                        synthetic.add(node);
                    }
                }

                @Override
                public void registerFlow(FlowValue ... nodes) {
                    for (FlowValue node : nodes) {
                        if (node.isComplex()) continue;
                        newFlows.add(node);
                    }
                }
            };
            for (FlowValue value : flows) {
                postProcessor.process(value, sink);
            }
            flows.removeAll(synthetic);
            for (FlowValue syntheticValue : synthetic) {
                syntheticValue.setParents(new FlowValue[0]);
            }
            flows.addAll(newFlows);
            for (FlowValue value : flows) {
                value.finish();
            }
            for (FlowValue value : flows) {
                value.onFinished();
            }
        }
        return flows;
    }

    public FlowValue newValue(Type type) {
        if (type == null) {
            return DummyFlowValue.UNINITIALIZED;
        }
        if (type == Type.VOID_TYPE) {
            return null;
        }
        return new DummyFlowValue(type);
    }

    public FlowValue newOperation(AbstractInsnNode insn) {
        Type type = ExpressionASMUtils.getNewType(insn);
        return this.recordFlow(type, insn, new FlowValue[0]);
    }

    public FlowValue copyOperation(AbstractInsnNode insn, FlowValue value) {
        switch (insn.getOpcode()) {
            case 89: 
            case 90: 
            case 91: 
            case 92: 
            case 93: 
            case 94: 
            case 95: {
                return value;
            }
            case 54: 
            case 55: 
            case 56: 
            case 57: 
            case 58: {
                this.recordFlow(Type.VOID_TYPE, insn, value);
                return new DummyFlowValue(value.getType());
            }
        }
        VarInsnNode varNode = (VarInsnNode)insn;
        Type type = this.localTypes.get(varNode);
        return this.recordFlow(type, insn, new FlowValue[0]);
    }

    public FlowValue unaryOperation(AbstractInsnNode insn, FlowValue value) {
        Type type = ExpressionASMUtils.getUnaryType(insn);
        if (insn.getOpcode() == 132) {
            this.recordFlow(Type.VOID_TYPE, insn, new FlowValue[0]);
            return new DummyFlowValue(type);
        }
        return this.recordFlow(type, insn, value);
    }

    public FlowValue binaryOperation(AbstractInsnNode insn, FlowValue value1, FlowValue value2) {
        if (insn.getOpcode() == 50 || insn.getOpcode() == 51) {
            return this.recordComputedFlow(1, inputs -> ExpressionASMUtils.getInnerType(inputs[0].getType()), insn, value1, value2);
        }
        Type type = ExpressionASMUtils.getBinaryType(insn, null);
        return this.recordFlow(type, insn, value1, value2);
    }

    public FlowValue ternaryOperation(AbstractInsnNode insn, FlowValue value1, FlowValue value2, FlowValue value3) {
        return this.recordFlow(Type.VOID_TYPE, insn, value1, value2, value3);
    }

    public FlowValue naryOperation(AbstractInsnNode insn, List<? extends FlowValue> values) {
        if (insn instanceof MethodInsnNode && Boxing.isBoxing((MethodInsnNode)insn)) {
            return values.get(0);
        }
        Type type = ExpressionASMUtils.getNaryType(insn);
        return this.recordFlow(type, insn, values.toArray(new FlowValue[0]));
    }

    public void returnOperation(AbstractInsnNode insn, FlowValue value, FlowValue expected) {
    }

    public FlowValue merge(FlowValue value1, FlowValue value2) {
        return value1.mergeWith(value2, this.context);
    }

    private FlowValue recordFlow(Type type, AbstractInsnNode insn, FlowValue ... inputs) {
        FlowValue cached = this.cache.get(insn);
        if (cached == null) {
            cached = new FlowValue(type, insn, inputs);
            this.cache.put(insn, cached);
        } else {
            cached.mergeInputs(inputs, this.context);
        }
        return cached;
    }

    private FlowValue recordComputedFlow(int size, Function<FlowValue[], Type> type, AbstractInsnNode insn, FlowValue ... inputs) {
        FlowValue cached = this.cache.get(insn);
        if (cached == null) {
            cached = new ComputedFlowValue(size, type, insn, inputs);
            this.cache.put(insn, cached);
        } else {
            cached.mergeInputs(inputs, this.context);
        }
        return cached;
    }
}

