package com.llamalad7.mixinextras.sugar.impl.ref;

import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefRuntime;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.sugar.impl.ref.generated.GeneratedImplDummy;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.ClassGenUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.ClassNode;

public class LocalRefClassGenerator {
    private static final String IMPL_PACKAGE = StringUtils.substringBeforeLast(LocalRefClassGenerator.class.getName(), ".").replace('.', '/') + "/generated";
    private static final Map<Class<?>, String> interfaceToImpl = new HashMap();

    public static String getForType(Type type) {
        Class<?> refInterface = LocalRefUtils.getInterfaceFor(type);
        String owner = interfaceToImpl.get(refInterface);
        if (owner != null) {
            return owner;
        }
        owner = IMPL_PACKAGE + '/' + StringUtils.substringAfterLast(refInterface.getName(), ".") + "Impl";
        String desc = type.getDescriptor();
        String innerDesc = desc.length() == 1 ? desc : Type.getDescriptor(Object.class);
        interfaceToImpl.put(refInterface, owner);
        ClassNode node = new ClassNode();
        node.visit(52, 49, owner, null, Type.getInternalName(Object.class), null);
        LocalRefClassGenerator.generateClass(node, owner, innerDesc, refInterface.getName());
        ClassGenUtils.defineClass(node, GeneratedImplDummy.getLookup());
        return owner;
    }

    private static void generateClass(ClassNode node, String owner, String innerDesc, String interfaceName) {
        Type objectType = ASMUtils.OBJECT_TYPE;
        Type innerType = Type.getType((String)innerDesc);
        for (String name : MixinExtrasService.getInstance().getAllClassNamesAtLeast(interfaceName, MixinExtrasVersion.V0_2_0_BETA_5)) {
            node.interfaces.add(name.replace('.', '/'));
        }
        node.visitField(2, "value", innerDesc, null, null);
        node.visitField(2, "state", "B", null, null);
        Consumer<InstructionAdapter> checkState = code -> {
            String runtime = Type.getInternalName(LocalRefRuntime.class);
            code.load(0, objectType);
            code.getfield(owner, "state", "B");
            Label passed = new Label();
            code.ifeq(passed);
            code.load(0, objectType);
            code.getfield(owner, "state", "B");
            code.invokestatic(runtime, "checkState", "(B)V", false);
            code.mark(passed);
        };
        LocalRefClassGenerator.genMethod((ClassVisitor)node, "<init>", "()V", code -> {
            code.load(0, objectType);
            code.invokespecial(objectType.getInternalName(), "<init>", "()V", false);
            code.load(0, objectType);
            code.iconst(1);
            code.putfield(owner, "state", "B");
            code.areturn(Type.VOID_TYPE);
        });
        LocalRefClassGenerator.genMethod((ClassVisitor)node, "get", "()" + innerDesc, code -> {
            checkState.accept((InstructionAdapter)code);
            code.load(0, objectType);
            code.getfield(owner, "value", innerDesc);
            code.areturn(innerType);
        });
        LocalRefClassGenerator.genMethod((ClassVisitor)node, "set", "(" + innerDesc + ")V", code -> {
            checkState.accept((InstructionAdapter)code);
            code.load(0, objectType);
            code.load(1, innerType);
            code.putfield(owner, "value", innerDesc);
            code.areturn(Type.VOID_TYPE);
        });
        LocalRefClassGenerator.genMethod((ClassVisitor)node, "init", "(" + innerDesc + ")V", code -> {
            code.load(0, objectType);
            code.load(1, innerType);
            code.putfield(owner, "value", innerDesc);
            code.load(0, objectType);
            code.iconst(0);
            code.putfield(owner, "state", "B");
            code.areturn(Type.VOID_TYPE);
        });
        LocalRefClassGenerator.genMethod((ClassVisitor)node, "dispose", "()" + innerDesc, code -> {
            checkState.accept((InstructionAdapter)code);
            code.load(0, objectType);
            code.iconst(2);
            code.putfield(owner, "state", "B");
            code.load(0, objectType);
            code.getfield(owner, "value", innerDesc);
            code.areturn(innerType);
        });
    }

    private static void genMethod(ClassVisitor cv, String name, String desc, Consumer<InstructionAdapter> code) {
        code.accept(new InstructionAdapter(cv.visitMethod(1, name, desc, null, null)));
    }
}

