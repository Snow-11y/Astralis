package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.StringConcatInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class StringConcatFactoryExpander
extends InsnExpander {
    private static final String STRING_CONCAT_FACTORY = "java/lang/invoke/StringConcatFactory";
    private static final Type STRING_BUILDER = Type.getType(StringBuilder.class);
    private static final Type STRING = Type.getType(String.class);

    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        AbstractInsnNode indy = node.getInsn();
        List<ConcatPart> parts = this.parseConcat(indy);
        if (parts == null) {
            return;
        }
        FlowValue current = null;
        FlowValue initialComponent = null;
        ArrayList<FlowValue> appendCalls = new ArrayList<FlowValue>();
        int nextArgument = 0;
        int finishedParts = 1;
        for (int i = 0; i < parts.size(); ++i) {
            FlowValue component;
            ConcatPart part = parts.get(i);
            if (part instanceof ConcatPart.Argument) {
                component = this.unwrapConcatArgument(node.getInput(nextArgument++), sink);
            } else {
                Object cst = part instanceof ConcatPart.PooledConstant ? ((ConcatPart.PooledConstant)part).value : ((ConcatPart.TemplateString)part).value;
                LdcInsnNode componentInsn = new LdcInsnNode(cst);
                component = new FlowValue(ExpressionASMUtils.getNewType((AbstractInsnNode)componentInsn), (AbstractInsnNode)componentInsn, new FlowValue[0]);
                this.registerComponent(component, part, indy);
                sink.registerFlow(component);
            }
            if (i == 0) {
                current = initialComponent = component;
                continue;
            }
            InsnNode newInsn = StringConcatFactoryExpander.dummyInsn();
            FlowValue[] newParents = new FlowValue[]{current, component};
            if (i == parts.size() - 1) {
                node.setInsn((AbstractInsnNode)newInsn);
                node.setParents(newParents);
                this.registerComponent(node, Component.TO_STRING, indy);
                continue;
            }
            current = new FlowValue(STRING_BUILDER, (AbstractInsnNode)newInsn, newParents);
            this.registerComponent(current, new PartialResult(finishedParts++), indy);
            sink.registerFlow(current);
            appendCalls.add(current);
        }
        this.decorateConcat(initialComponent, appendCalls, node);
    }

    private void decorateConcat(FlowValue initialComponent, List<FlowValue> appendCalls, FlowValue toStringCall) {
        boolean isFirstConcat = true;
        for (FlowValue append : appendCalls) {
            append.decorate("stringConcatInfo", new StringConcatInfo(isFirstConcat, true, initialComponent, null));
            isFirstConcat = false;
        }
        toStringCall.decorate("stringConcatInfo", new StringConcatInfo(isFirstConcat, false, initialComponent, null));
    }

    private FlowValue unwrapConcatArgument(FlowValue argument, FlowPostProcessor.OutputSink sink) {
        if (!argument.isComplex() && this.isStringValueOf(argument.getInsn())) {
            sink.markAsSynthetic(argument);
            return argument.getInput(0);
        }
        return argument;
    }

    private boolean isStringValueOf(AbstractInsnNode insn) {
        if (insn.getOpcode() != 184) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode)insn;
        return call.owner.equals(STRING.getInternalName()) && call.name.equals("valueOf") && call.desc.equals("(Ljava/lang/Object;)Ljava/lang/String;");
    }

    @Override
    public void expand(Target target, InjectionNodes.InjectionNode node, InsnExpander.Expansion expansion) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode)node.getCurrentTarget();
        Set<InsnExpander.InsnComponent> interests = expansion.registeredInterests();
        if (interests.size() == 1 && interests.iterator().next() == Component.TO_STRING) {
            expansion.registerInsn(Component.TO_STRING, node.getCurrentTarget());
            return;
        }
        ArrayList<Object> insns = new ArrayList<Object>();
        Type[] argTypes = Type.getArgumentTypes((String)indy.desc);
        int[] argMap = this.storeArgs(target, argTypes, insns::add);
        insns.add(this.makeNewBuilder());
        insns.add(new InsnNode(89));
        target.method.maxStack += 2;
        insns.add(this.makeBuilderInit());
        int nextArgument = 0;
        int finishedParts = 0;
        for (ConcatPart part : this.parseConcat((AbstractInsnNode)indy)) {
            Type partType;
            if (part instanceof ConcatPart.Argument) {
                int arg = nextArgument++;
                partType = argTypes[arg];
                insns.add(new VarInsnNode(partType.getOpcode(21), argMap[arg]));
            } else {
                Object cst = part instanceof ConcatPart.PooledConstant ? ((ConcatPart.PooledConstant)part).value : ((ConcatPart.TemplateString)part).value;
                AbstractInsnNode componentInsn = expansion.registerInsn(part, (AbstractInsnNode)new LdcInsnNode(cst));
                partType = ExpressionASMUtils.getNewType(componentInsn);
                target.method.maxStack += partType.getSize();
                insns.add(componentInsn);
            }
            insns.add(expansion.registerInsn(new PartialResult(finishedParts++), this.makeAppendCall(partType)));
        }
        insns.add(expansion.registerInsn(Component.TO_STRING, this.makeToStringCall()));
        this.expandInsn(target, node, insns.toArray(new AbstractInsnNode[0]));
    }

    private List<ConcatPart> parseConcat(AbstractInsnNode insn) {
        if (!(insn instanceof InvokeDynamicInsnNode)) {
            return null;
        }
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode)insn;
        if (!indy.bsm.getOwner().equals(STRING_CONCAT_FACTORY)) {
            return null;
        }
        if (indy.bsm.getName().equals("makeConcat")) {
            int inputCount = Type.getArgumentTypes((String)indy.desc).length;
            return this.parseConcatWithConstants(new Object[]{StringUtils.repeat('\u0001', inputCount)});
        }
        if (indy.bsm.getName().equals("makeConcatWithConstants")) {
            return this.parseConcatWithConstants(indy.bsmArgs);
        }
        return null;
    }

    private AbstractInsnNode makeNewBuilder() {
        return new TypeInsnNode(187, STRING_BUILDER.getInternalName());
    }

    private AbstractInsnNode makeBuilderInit() {
        return new MethodInsnNode(183, STRING_BUILDER.getInternalName(), "<init>", "()V", false);
    }

    private AbstractInsnNode makeAppendCall(Type type) {
        if (type.getSort() == 10) {
            type = ExpressionASMUtils.OBJECT_TYPE;
        }
        return new MethodInsnNode(182, STRING_BUILDER.getInternalName(), "append", Type.getMethodDescriptor((Type)STRING_BUILDER, (Type[])new Type[]{type}), false);
    }

    private AbstractInsnNode makeToStringCall() {
        return new MethodInsnNode(182, STRING_BUILDER.getInternalName(), "toString", Type.getMethodDescriptor((Type)STRING, (Type[])new Type[0]), false);
    }

    private List<ConcatPart> parseConcatWithConstants(Object[] bsmArgs) {
        String template = (String)bsmArgs[0];
        ArrayList<ConcatPart> result = new ArrayList<ConcatPart>();
        int id = 0;
        int nextCst = 1;
        StringBuilder currentString = null;
        block4: for (int i = 0; i < template.length(); ++i) {
            char c = template.charAt(i);
            if ((c == '\u0001' || c == '\u0002') && currentString != null) {
                result.add(new ConcatPart.TemplateString(id++, currentString.toString()));
                currentString = null;
            }
            switch (c) {
                case '\u0001': {
                    result.add(new ConcatPart.Argument(id++));
                    continue block4;
                }
                case '\u0002': {
                    result.add(new ConcatPart.PooledConstant(id++, bsmArgs[nextCst++]));
                    continue block4;
                }
                default: {
                    if (currentString == null) {
                        currentString = new StringBuilder();
                    }
                    currentString.append(c);
                }
            }
        }
        if (currentString != null) {
            result.add(new ConcatPart.TemplateString(id, currentString.toString()));
        }
        return result;
    }

    private int[] storeArgs(Target target, Type[] args, Consumer<AbstractInsnNode> add) {
        int[] map = new int[args.length];
        for (int i = args.length - 1; i >= 0; --i) {
            Type type = args[i];
            int index = target.allocateLocals(type.getSize());
            target.addLocalVariable(index, "concatTemp" + index, type.getDescriptor());
            map[i] = index;
            add.accept((AbstractInsnNode)new VarInsnNode(type.getOpcode(54), index));
        }
        return map;
    }

    private static abstract class ConcatPart
    implements InsnExpander.InsnComponent {
        private final int id;

        private ConcatPart(int id) {
            this.id = id;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            ConcatPart that = (ConcatPart)o;
            return this.id == that.id;
        }

        public int hashCode() {
            return Objects.hash(this.getClass(), this.id);
        }

        public static class TemplateString
        extends ConcatPart {
            public final String value;

            public TemplateString(int id, String value) {
                super(id);
                this.value = value;
            }
        }

        public static class PooledConstant
        extends ConcatPart {
            public final Object value;

            public PooledConstant(int id, Object value) {
                super(id);
                this.value = value;
            }
        }

        public static class Argument
        extends ConcatPart {
            public Argument(int id) {
                super(id);
            }
        }
    }

    private static enum Component implements InsnExpander.InsnComponent
    {
        TO_STRING;

    }

    private static class PartialResult
    implements InsnExpander.InsnComponent {
        public final int finishedParts;

        private PartialResult(int finishedParts) {
            this.finishedParts = finishedParts;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            PartialResult that = (PartialResult)o;
            return this.finishedParts == that.finishedParts;
        }

        public int hashCode() {
            return Objects.hash(this.getClass(), this.finishedParts);
        }
    }
}

