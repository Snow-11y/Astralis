package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.throwables.InvalidAccessorException;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.MethodNodeEx;

class InvokerInfo
extends AccessorInfo {
    InvokerInfo(MixinTargetContext mixin, MethodNode method) {
        super(mixin, method, Invoker.class);
    }

    @Override
    protected AccessorInfo.AccessorType initType() {
        if (this.specifiedName != null) {
            String mappedReference = this.mixin.getReferenceMapper().remap(this.mixin.getClassRef(), this.specifiedName);
            return this.initType(mappedReference.replace('.', '/'), this.mixin.getTargetClassRef());
        }
        AccessorInfo.AccessorName accessorName = AccessorInfo.AccessorName.of(MethodNodeEx.getName(this.method), false);
        if (accessorName != null) {
            for (String prefix : AccessorInfo.AccessorType.OBJECT_FACTORY.getExpectedPrefixes()) {
                if (!prefix.equals(accessorName.prefix)) continue;
                return this.initType(accessorName.name, this.mixin.getTargetClassInfo().getSimpleName());
            }
        }
        return AccessorInfo.AccessorType.METHOD_PROXY;
    }

    private AccessorInfo.AccessorType initType(String targetName, String targetClassName) {
        if ("<init>".equals(targetName) || targetClassName.equals(targetName)) {
            if (!this.returnType.equals((Object)this.mixin.getTargetClassInfo().getType())) {
                throw new InvalidAccessorException((IMixinContext)this.mixin, String.format("%s appears to have an invalid return type. %s requires matching return type. Found %s expected %s", new Object[]{this, AccessorInfo.AccessorType.OBJECT_FACTORY, Bytecode.getSimpleName(this.returnType), this.mixin.getTargetClassInfo().getSimpleName()}));
            }
            if (!this.isStatic) {
                throw new InvalidAccessorException((IMixinContext)this.mixin, String.format("%s for %s must be static", new Object[]{this, AccessorInfo.AccessorType.OBJECT_FACTORY, Bytecode.getSimpleName(this.returnType)}));
            }
            return AccessorInfo.AccessorType.OBJECT_FACTORY;
        }
        return AccessorInfo.AccessorType.METHOD_PROXY;
    }

    @Override
    protected Type initTargetFieldType() {
        return null;
    }

    @Override
    protected ITargetSelector initTarget() {
        if (this.type == AccessorInfo.AccessorType.OBJECT_FACTORY) {
            return new MemberInfo("<init>", null, Bytecode.changeDescriptorReturnType(this.method.desc, "V"));
        }
        return new MemberInfo(this.getTargetName(this.specifiedName), null, this.method.desc);
    }

    @Override
    public void locate() {
        this.targetMethod = this.findTargetMethod();
    }

    private MethodNode findTargetMethod() {
        TargetSelector.Result result = TargetSelector.run(this.target.configure(ITargetSelector.Configure.ORPHAN, new String[0]), ElementNode.methodList(this.classNode));
        try {
            return (MethodNode)result.getSingleResult(true);
        }
        catch (IllegalStateException ex) {
            String name;
            String message = String.format("%s matching %s in %s for %s", ex.getMessage(), this.target, this.classNode.name, this);
            if (this.type == AccessorInfo.AccessorType.METHOD_PROXY && this.specifiedName != null && this.target instanceof ITargetSelectorByName && (name = ((ITargetSelectorByName)this.target).getName()) != null && (name.contains(".") || name.contains("/"))) {
                throw new InvalidAccessorException((AccessorInfo)this, "Invalid factory invoker failed to match the target class. " + message);
            }
            throw new InvalidAccessorException((AccessorInfo)this, message);
        }
    }
}

