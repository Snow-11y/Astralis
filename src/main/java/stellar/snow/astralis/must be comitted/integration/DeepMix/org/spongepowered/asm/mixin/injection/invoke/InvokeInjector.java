package org.spongepowered.asm.mixin.injection.invoke;

import java.util.List;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;

public abstract class InvokeInjector
extends Injector {
    public InvokeInjector(InjectionInfo info, String annotationType) {
        super(info, annotationType);
    }

    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        this.checkTarget(target);
    }

    protected void checkTarget(Target target) {
        this.checkTargetModifiers(target, true);
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        if (!(node.getCurrentTarget() instanceof MethodInsnNode)) {
            throw new InvalidInjectionException((ISelectorContext)this.info, String.format("%s annotation on is targetting a non-method insn in %s in %s", this.annotationType, target, this));
        }
        this.injectAtInvoke(target, node);
    }

    protected abstract void injectAtInvoke(Target var1, InjectionNodes.InjectionNode var2);
}

