package org.spongepowered.asm.mixin.injection.struct;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Counter;
import org.spongepowered.asm.util.Locals;

public class Target
implements Comparable<Target>,
Iterable<AbstractInsnNode> {
    public final ClassInfo classInfo;
    public final ClassNode classNode;
    public final MethodNode method;
    public final InsnList insns;
    public final boolean isStatic;
    public final Type[] arguments;
    public final Type returnType;
    private final int maxStack;
    private final int maxLocals;
    private final InjectionNodes injectionNodes = new InjectionNodes();
    private String callbackInfoClass;
    private String callbackDescriptor;
    private int[] argIndices;
    private List<Integer> argMapVars;
    private LabelNode start;
    private LabelNode end;

    Target(ClassInfo classInfo, ClassNode classNode, MethodNode method) {
        this.classInfo = classInfo;
        this.classNode = classNode;
        this.method = method;
        this.insns = method.instructions;
        this.isStatic = Bytecode.isStatic(method);
        this.arguments = Type.getArgumentTypes((String)method.desc);
        this.returnType = Type.getReturnType((String)method.desc);
        this.maxStack = method.maxStack;
        this.maxLocals = method.maxLocals;
    }

    public InjectionNodes.InjectionNode addInjectionNode(AbstractInsnNode node) {
        return this.injectionNodes.add(node);
    }

    public InjectionNodes.InjectionNode getInjectionNode(AbstractInsnNode node) {
        return this.injectionNodes.get(node);
    }

    public String getName() {
        return this.method.name;
    }

    public String getDesc() {
        return this.method.desc;
    }

    public String getSignature() {
        return this.method.signature;
    }

    public int getMaxLocals() {
        return this.maxLocals;
    }

    public int getMaxStack() {
        return this.maxStack;
    }

    public int getCurrentMaxLocals() {
        return this.method.maxLocals;
    }

    public int getCurrentMaxStack() {
        return this.method.maxStack;
    }

    public int allocateLocal() {
        return this.allocateLocals(1);
    }

    public int allocateLocals(int locals) {
        int nextLocal = this.method.maxLocals;
        this.method.maxLocals += locals;
        return nextLocal;
    }

    public Extension extendLocals() {
        return new Extension(true);
    }

    public Extension extendStack() {
        return new Extension(false);
    }

    void extendLocalsBy(int locals) {
        this.setMaxLocals(this.maxLocals + locals);
    }

    private void setMaxLocals(int maxLocals) {
        if (maxLocals > this.method.maxLocals) {
            this.method.maxLocals = maxLocals;
        }
    }

    void extendStackBy(int stack) {
        this.setMaxStack(this.maxStack + stack);
    }

    private void setMaxStack(int maxStack) {
        if (maxStack > this.method.maxStack) {
            this.method.maxStack = maxStack;
        }
    }

    public int[] generateArgMap(Type[] args, int start) {
        return this.generateArgMap(args, start, false);
    }

    public int[] generateArgMap(Type[] args, int start, boolean fresh) {
        if (this.argMapVars == null) {
            this.argMapVars = new ArrayList<Integer>();
        }
        int[] argMap = new int[args.length];
        Counter index = new Counter();
        for (int arg = start; arg < args.length; ++arg) {
            int size = args[arg].getSize();
            if (fresh) {
                argMap[arg] = this.allocateLocals(size);
                index.value += size;
                continue;
            }
            argMap[arg] = this.allocateArgMapLocal(index, size);
        }
        return argMap;
    }

    private int allocateArgMapLocal(Counter index, int size) {
        boolean isLastSlotFree;
        boolean bl = isLastSlotFree = index.value < this.argMapVars.size();
        while (index.value < this.argMapVars.size()) {
            int local = this.argMapVars.get(index.value);
            if (size == 1) {
                ++index.value;
                return local;
            }
            int nextIndex = index.value + 1;
            if (nextIndex < this.argMapVars.size() && local + 1 == this.argMapVars.get(nextIndex)) {
                index.value += 2;
                return local;
            }
            ++index.value;
        }
        int newLocal = this.allocateLocal();
        this.argMapVars.add(newLocal);
        ++index.value;
        if (size == 1) {
            return newLocal;
        }
        if (isLastSlotFree && newLocal == this.argMapVars.get(this.argMapVars.size() - 2) + 1) {
            return newLocal - 1;
        }
        this.argMapVars.add(this.allocateLocal());
        ++index.value;
        return newLocal;
    }

    public int[] getArgIndices() {
        if (this.argIndices == null) {
            this.argIndices = this.calcArgIndices(this.isStatic ? 0 : 1);
        }
        return this.argIndices;
    }

    private int[] calcArgIndices(int local) {
        int[] argIndices = new int[this.arguments.length];
        for (int arg = 0; arg < this.arguments.length; ++arg) {
            argIndices[arg] = local;
            local += this.arguments[arg].getSize();
        }
        return argIndices;
    }

    public String getCallbackInfoClass() {
        if (this.callbackInfoClass == null) {
            this.callbackInfoClass = CallbackInfo.getCallInfoClassName(this.returnType);
        }
        return this.callbackInfoClass;
    }

    public String getSimpleCallbackDescriptor() {
        return String.format("(L%s;)V", this.getCallbackInfoClass());
    }

    public String getCallbackDescriptor(Type[] locals, Type[] argumentTypes) {
        return this.getCallbackDescriptor(false, locals, argumentTypes, 0, Short.MAX_VALUE);
    }

    public String getCallbackDescriptor(boolean captureLocals, Type[] locals, Type[] argumentTypes, int startIndex, int extra) {
        if (this.callbackDescriptor == null) {
            this.callbackDescriptor = String.format("(%sL%s;)V", this.getDesc().substring(1, this.getDesc().indexOf(41)), this.getCallbackInfoClass());
        }
        if (!captureLocals || locals == null) {
            return this.callbackDescriptor;
        }
        StringBuilder descriptor = new StringBuilder(this.callbackDescriptor.substring(0, this.callbackDescriptor.indexOf(41)));
        for (int l = startIndex; l < locals.length && extra > 0; ++l) {
            if (locals[l] == null) continue;
            descriptor.append(locals[l].getDescriptor());
            --extra;
        }
        return descriptor.append(")V").toString();
    }

    public String toString() {
        return String.format("%s::%s%s", this.classNode.name, this.getName(), this.getDesc());
    }

    @Override
    public int compareTo(Target o) {
        if (o == null) {
            return Integer.MAX_VALUE;
        }
        return this.toString().compareTo(o.toString());
    }

    public int indexOf(InjectionNodes.InjectionNode node) {
        return this.insns.indexOf(node.getCurrentTarget());
    }

    public int indexOf(AbstractInsnNode insn) {
        return this.insns.indexOf(insn);
    }

    public AbstractInsnNode get(int index) {
        return this.insns.get(index);
    }

    @Override
    public Iterator<AbstractInsnNode> iterator() {
        return this.insns.iterator();
    }

    public MethodInsnNode findInitNodeFor(TypeInsnNode newNode) {
        return this.findInitNodeFor(newNode, null);
    }

    public MethodInsnNode findInitNodeFor(TypeInsnNode newNode, String desc) {
        return BeforeNew.findInitNodeFor(this.insns, newNode, desc);
    }

    public void insert(InjectionNodes.InjectionNode location, InsnList insns) {
        this.insns.insert(location.getCurrentTarget(), insns);
    }

    public void insert(InjectionNodes.InjectionNode location, AbstractInsnNode insn) {
        this.insns.insert(location.getCurrentTarget(), insn);
    }

    public void insert(AbstractInsnNode location, InsnList insns) {
        this.insns.insert(location, insns);
    }

    public void insert(AbstractInsnNode location, AbstractInsnNode insn) {
        this.insns.insert(location, insn);
    }

    public void insertBefore(InjectionNodes.InjectionNode location, InsnList insns) {
        this.insns.insertBefore(location.getCurrentTarget(), insns);
    }

    public void insertBefore(InjectionNodes.InjectionNode location, AbstractInsnNode insn) {
        this.insns.insertBefore(location.getCurrentTarget(), insn);
    }

    public void insertBefore(AbstractInsnNode location, InsnList insns) {
        this.insns.insertBefore(location, insns);
    }

    public void insertBefore(AbstractInsnNode location, AbstractInsnNode insn) {
        this.insns.insertBefore(location, insn);
    }

    public void replaceNode(AbstractInsnNode location, AbstractInsnNode insn) {
        this.insns.insertBefore(location, insn);
        this.insns.remove(location);
        this.injectionNodes.replace(location, insn);
    }

    public void replaceNode(AbstractInsnNode location, AbstractInsnNode champion, InsnList insns) {
        this.insns.insertBefore(location, insns);
        this.insns.remove(location);
        this.injectionNodes.replace(location, champion);
    }

    public void wrapNode(AbstractInsnNode location, AbstractInsnNode champion, InsnList before, InsnList after) {
        this.insns.insertBefore(location, before);
        this.insns.insert(location, after);
        this.injectionNodes.replace(location, champion);
    }

    public void replaceNode(AbstractInsnNode location, InsnList insns) {
        this.insns.insertBefore(location, insns);
        this.removeNode(location);
    }

    public void removeNode(AbstractInsnNode insn) {
        this.insns.remove(insn);
        this.injectionNodes.remove(insn);
    }

    public void addLocalVariable(int index, String name, String desc) {
        this.addLocalVariable(index, name, desc, null, null);
    }

    public void addLocalVariable(int index, String name, String desc, LabelNode from, LabelNode to) {
        if (from == null) {
            from = this.getStartLabel();
        }
        if (to == null) {
            to = this.getEndLabel();
        }
        if (this.method.localVariables == null) {
            this.method.localVariables = new ArrayList();
        }
        Iterator iter = this.method.localVariables.iterator();
        while (iter.hasNext()) {
            LocalVariableNode local = (LocalVariableNode)iter.next();
            if (local == null || local.index != index || from != local.start || to != local.end) continue;
            iter.remove();
        }
        this.method.localVariables.add(new Locals.SyntheticLocalVariableNode(name, desc, null, from, to, index));
    }

    private LabelNode getStartLabel() {
        if (this.start == null) {
            this.start = new LabelNode();
            this.insns.insert((AbstractInsnNode)this.start);
        }
        return this.start;
    }

    private LabelNode getEndLabel() {
        if (this.end == null) {
            this.end = new LabelNode();
            this.insns.add((AbstractInsnNode)this.end);
        }
        return this.end;
    }

    public static Target of(ClassInfo classInfo, ClassNode classNode, MethodNode method) {
        if (method.name.equals("<init>")) {
            return new Constructor(classInfo, classNode, method);
        }
        return new Target(classInfo, classNode, method);
    }

    public class Extension {
        private final boolean locals;
        private int size;

        Extension(boolean locals) {
            this.locals = locals;
        }

        public Extension add() {
            ++this.size;
            return this;
        }

        public Extension add(int size) {
            this.size += size;
            return this;
        }

        public Extension add(Type[] types) {
            return this.add(Bytecode.getArgsSize(types));
        }

        public Extension set(int size) {
            this.size = size;
            return this;
        }

        public int get() {
            return this.size;
        }

        public void apply() {
            if (this.locals) {
                Target.this.extendLocalsBy(this.size);
            } else {
                Target.this.extendStackBy(this.size);
            }
            this.size = 0;
        }
    }
}

