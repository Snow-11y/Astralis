package org.spongepowered.asm.mixin.injection.code;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.mixin.injection.code.IInsnListEx;
import org.spongepowered.asm.mixin.injection.code.InsnListReadOnly;
import org.spongepowered.asm.mixin.injection.struct.Constructor;
import org.spongepowered.asm.mixin.injection.struct.IChainedDecoration;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.struct.Initialiser;
import org.spongepowered.asm.util.Bytecode;

public class InsnListEx
extends InsnListReadOnly
implements IInsnListEx {
    protected final Target target;
    private Map<String, Object> decorations;

    public InsnListEx(Target target) {
        super(target.insns);
        this.target = target;
    }

    public String toString() {
        return this.target.toString();
    }

    @Override
    public String getTargetName() {
        return this.target.getName();
    }

    @Override
    public String getTargetDesc() {
        return this.target.getDesc();
    }

    @Override
    public String getTargetSignature() {
        return this.target.getSignature();
    }

    @Override
    public int getTargetAccess() {
        return this.target.method.access;
    }

    @Override
    public boolean isTargetStatic() {
        return this.target.isStatic;
    }

    @Override
    public boolean isTargetConstructor() {
        return this.target instanceof Constructor;
    }

    @Override
    public boolean isTargetStaticInitialiser() {
        return "<clinit>".equals(this.target.getName());
    }

    @Override
    public AbstractInsnNode getSpecialNode(IInsnListEx.SpecialNodeType type) {
        switch (type) {
            case DELEGATE_CTOR: {
                if (this.target instanceof Constructor) {
                    Bytecode.DelegateInitialiser superCall = ((Constructor)this.target).findDelegateInitNode();
                    if (superCall.isPresent && this.contains((AbstractInsnNode)superCall.insn)) {
                        return superCall.insn;
                    }
                }
                return null;
            }
            case INITIALISER_INJECTION_POINT: {
                Initialiser.InjectionMode mode;
                AbstractInsnNode initialiserInjectionPoint;
                if (this.target instanceof Constructor && this.contains(initialiserInjectionPoint = ((Constructor)this.target).findInitialiserInjectionPoint(mode = Initialiser.InjectionMode.DEFAULT))) {
                    return initialiserInjectionPoint;
                }
                return null;
            }
            case CTOR_BODY: {
                AbstractInsnNode beforeBody;
                if (this.target instanceof Constructor && this.contains(beforeBody = ((Constructor)this.target).findFirstBodyInsn())) {
                    return beforeBody;
                }
                return null;
            }
        }
        return null;
    }

    public <V> InsnListEx decorate(String key, V value) {
        Object previous;
        if (this.decorations == null) {
            this.decorations = new HashMap<String, Object>();
        }
        if (value instanceof IChainedDecoration && this.decorations.containsKey(key) && (previous = this.decorations.get(key)).getClass().equals(value.getClass())) {
            ((IChainedDecoration)value).replace(previous);
        }
        this.decorations.put(key, value);
        return this;
    }

    public InsnListEx undecorate(String key) {
        if (this.decorations != null) {
            this.decorations.remove(key);
        }
        return this;
    }

    public InsnListEx undecorate() {
        this.decorations = null;
        return this;
    }

    @Override
    public boolean hasDecoration(String key) {
        return this.decorations != null && this.decorations.get(key) != null;
    }

    @Override
    public <V> V getDecoration(String key) {
        return (V)(this.decorations == null ? null : this.decorations.get(key));
    }

    @Override
    public <V> V getDecoration(String key, V defaultValue) {
        Object existing = this.decorations == null ? null : this.decorations.get(key);
        return (V)(existing != null ? existing : defaultValue);
    }
}

