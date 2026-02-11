package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.IInsnListEx;
import org.spongepowered.asm.mixin.injection.points.MethodHead;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionPointException;
import org.spongepowered.asm.service.MixinService;

@InjectionPoint.AtCode(value="CTOR_HEAD")
public class ConstructorHead
extends MethodHead {
    protected final ILogger logger = MixinService.getService().getLogger("mixin");
    private final Enforce enforce;
    private final boolean verbose;
    private final MethodNode method;

    public ConstructorHead(InjectionPointData data) {
        super(data);
        if (!data.isUnsafe()) {
            throw new InvalidInjectionPointException(data.getMixin(), "@At(\"CTOR_HEAD\") requires unsafe=true", new Object[0]);
        }
        this.enforce = data.get("enforce", Enforce.DEFAULT);
        this.verbose = data.getMixin().getOption(MixinEnvironment.Option.DEBUG_VERBOSE);
        this.method = data.getMethod();
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        AbstractInsnNode postDelegate;
        if (!(insns instanceof IInsnListEx)) {
            return false;
        }
        IInsnListEx xinsns = (IInsnListEx)insns;
        if (!xinsns.isTargetConstructor()) {
            return super.find(desc, insns, nodes);
        }
        AbstractInsnNode delegateCtor = xinsns.getSpecialNode(IInsnListEx.SpecialNodeType.DELEGATE_CTOR);
        AbstractInsnNode abstractInsnNode = postDelegate = delegateCtor != null ? delegateCtor.getNext() : null;
        if (this.enforce == Enforce.POST_DELEGATE) {
            if (postDelegate == null) {
                if (this.verbose) {
                    this.logger.warn("@At(\"{}\") on {}{} targetting {} failed for enforce=POST_DELEGATE because no delegate was found", this.getAtCode(), this.method.name, this.method.desc, xinsns);
                }
                return false;
            }
            nodes.add(postDelegate);
            return true;
        }
        IInsnListEx.SpecialNodeType type = this.enforce == Enforce.PRE_BODY ? IInsnListEx.SpecialNodeType.CTOR_BODY : IInsnListEx.SpecialNodeType.INITIALISER_INJECTION_POINT;
        AbstractInsnNode postInit = xinsns.getSpecialNode(type);
        if (postInit != null) {
            nodes.add(postInit);
            return true;
        }
        if (postDelegate != null) {
            nodes.add(postDelegate);
            return true;
        }
        return super.find(desc, insns, nodes);
    }

    static enum Enforce {
        DEFAULT,
        POST_DELEGATE,
        POST_INIT,
        PRE_BODY;

    }
}

