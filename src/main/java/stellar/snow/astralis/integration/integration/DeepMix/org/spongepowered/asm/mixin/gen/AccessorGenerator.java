package org.spongepowered.asm.mixin.gen;

import java.util.ArrayList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.asm.ASM;

public abstract class AccessorGenerator {
    protected final AccessorInfo info;
    protected final boolean targetIsStatic;
    protected final boolean targetIsInterface;

    public AccessorGenerator(AccessorInfo info, boolean isStatic) {
        this.info = info;
        this.targetIsStatic = isStatic;
        this.targetIsInterface = info.getTargetClassInfo().isInterface();
    }

    protected void checkModifiers() {
        if (this.info.isStatic() != this.targetIsStatic) {
            if (!this.targetIsStatic) {
                throw new InvalidAccessorException(this.info, String.format("%s is invalid. Accessor method is static but the target is not.", this.info));
            }
            MixinService.getService().getLogger("mixin").info("{} should be static as its target is", this.info);
        }
    }

    protected final MethodNode createMethod(int maxLocals, int maxStack) {
        MethodNode method = this.info.getMethod();
        MethodNode accessor = new MethodNode(ASM.API_VERSION, method.access & 0xFFFFFBFF | 0x1000, method.name, method.desc, null, null);
        accessor.visibleAnnotations = new ArrayList();
        accessor.visibleAnnotations.add(this.info.getAnnotationNode());
        accessor.maxLocals = maxLocals;
        accessor.maxStack = maxStack;
        return accessor;
    }

    public void validate() {
    }

    public abstract MethodNode generate();
}

