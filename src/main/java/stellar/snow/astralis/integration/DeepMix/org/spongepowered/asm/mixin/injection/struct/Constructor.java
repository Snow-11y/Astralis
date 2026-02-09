package org.spongepowered.asm.mixin.injection.struct;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.mixin.transformer.struct.InsnRange;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.MarkerNode;

public class Constructor
extends Target {
    private Bytecode.DelegateInitialiser delegateInitialiser;
    private MarkerNode initialiserInjectionPoint;
    private MarkerNode bodyStart;
    private final String targetName;
    private final String targetSuperName;
    private Set<String> mixinInitialisedFields = new HashSet<String>();

    public Constructor(ClassInfo classInfo, ClassNode classNode, MethodNode method) {
        super(classInfo, classNode, method);
        this.targetName = this.classInfo.getName();
        this.targetSuperName = this.classInfo.getSuperName();
    }

    public Bytecode.DelegateInitialiser findDelegateInitNode() {
        if (this.delegateInitialiser == null) {
            this.delegateInitialiser = Bytecode.findDelegateInit(this.method, this.classInfo.getSuperName(), this.classNode.name);
        }
        return this.delegateInitialiser;
    }

    public boolean isInjectable() {
        Bytecode.DelegateInitialiser delegateInit = this.findDelegateInitNode();
        return !delegateInit.isPresent || delegateInit.isSuper;
    }

    public void inspect(Initialiser initialiser) {
        if (this.initialiserInjectionPoint != null) {
            throw new IllegalStateException("Attempted to inspect an incoming initialiser after the injection point was already determined");
        }
        for (AbstractInsnNode initialiserInsn : initialiser.getInsns()) {
            if (initialiserInsn.getOpcode() != 181) continue;
            this.mixinInitialisedFields.add(Constructor.fieldKey((FieldInsnNode)initialiserInsn));
        }
    }

    public AbstractInsnNode findInitialiserInjectionPoint(Initialiser.InjectionMode mode) {
        if (this.initialiserInjectionPoint != null) {
            return this.initialiserInjectionPoint;
        }
        AbstractInsnNode lastInsn = null;
        for (AbstractInsnNode insn : this.insns) {
            String key;
            if (insn.getOpcode() == 183 && "<init>".equals(((MethodInsnNode)insn).name)) {
                String owner = ((MethodInsnNode)insn).owner;
                if (!owner.equals(this.targetName) && !owner.equals(this.targetSuperName)) continue;
                lastInsn = insn;
                if (mode != Initialiser.InjectionMode.SAFE) continue;
                break;
            }
            if (insn.getOpcode() != 181 || mode != Initialiser.InjectionMode.DEFAULT || !this.mixinInitialisedFields.contains(key = Constructor.fieldKey((FieldInsnNode)insn))) continue;
            lastInsn = insn;
        }
        if (lastInsn == null) {
            return null;
        }
        this.initialiserInjectionPoint = new MarkerNode(1);
        this.insert(lastInsn, (AbstractInsnNode)this.initialiserInjectionPoint);
        return this.initialiserInjectionPoint;
    }

    public AbstractInsnNode findFirstBodyInsn() {
        if (this.bodyStart == null) {
            this.bodyStart = new MarkerNode(2);
            InsnRange range = Constructor.getRange(this.method);
            if (range.isValid()) {
                Deque<AbstractInsnNode> body = range.apply(this.insns, true);
                this.insertBefore(body.pop(), (AbstractInsnNode)this.bodyStart);
            } else if (range.marker > -1) {
                this.insert(this.insns.get(range.marker), (AbstractInsnNode)this.bodyStart);
            } else {
                this.bodyStart = null;
            }
        }
        return this.bodyStart;
    }

    private static String fieldKey(FieldInsnNode fieldNode) {
        return String.format("%s:%s", fieldNode.desc, fieldNode.name);
    }

    public static InsnRange getRange(MethodNode ctor) {
        boolean lineNumberIsValid = false;
        AbstractInsnNode endReturn = null;
        int line = 0;
        int start = 0;
        int end = 0;
        int delegateIndex = -1;
        for (AbstractInsnNode insn : ctor.instructions) {
            if (insn instanceof LineNumberNode) {
                line = ((LineNumberNode)insn).line;
                lineNumberIsValid = true;
                continue;
            }
            if (insn instanceof MethodInsnNode) {
                if (insn.getOpcode() != 183 || !"<init>".equals(((MethodInsnNode)insn).name) || delegateIndex != -1) continue;
                delegateIndex = ctor.instructions.indexOf(insn);
                start = line;
                continue;
            }
            if (insn.getOpcode() == 181) {
                lineNumberIsValid = false;
                continue;
            }
            if (insn.getOpcode() != 177) continue;
            if (lineNumberIsValid) {
                end = line;
                continue;
            }
            end = start;
            endReturn = insn;
        }
        if (endReturn != null) {
            LabelNode label = new LabelNode(new Label());
            ctor.instructions.insertBefore(endReturn, (AbstractInsnNode)label);
            ctor.instructions.insertBefore(endReturn, (AbstractInsnNode)new LineNumberNode(start, label));
        }
        return new InsnRange(start, end, delegateIndex);
    }
}

