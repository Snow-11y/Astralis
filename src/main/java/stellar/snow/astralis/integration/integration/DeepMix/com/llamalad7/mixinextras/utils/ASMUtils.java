package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

public class ASMUtils {
    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final Handle LMF_HANDLE = new Handle(6, "java/lang/invoke/LambdaMetafactory", "metafactory", Bytecode.generateDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class), false);

    public static String annotationToString(AnnotationNode annotation) {
        StringBuilder builder = new StringBuilder("@").append(ASMUtils.typeToString(Type.getType((String)annotation.desc)));
        List values = annotation.values;
        if (values == null || values.isEmpty()) {
            return builder.toString();
        }
        builder.append('(');
        for (int i = 0; i < values.size(); i += 2) {
            if (i != 0) {
                builder.append(", ");
            }
            String name = (String)values.get(i);
            Object value = values.get(i + 1);
            builder.append(name).append(" = ").append(ASMUtils.valueToString(value));
        }
        builder.append(')');
        return builder.toString();
    }

    public static String typeToString(Type type) {
        String name = type.getClassName();
        return name.substring(name.lastIndexOf(46) + 1).replace('$', '.');
    }

    private static String valueToString(Object value) {
        if (value instanceof String) {
            return '\"' + value.toString() + '\"';
        }
        if (value instanceof Type) {
            Type type = (Type)value;
            return ASMUtils.typeToString(type) + ".class";
        }
        if (value instanceof String[]) {
            String[] enumInfo = (String[])value;
            return ASMUtils.typeToString(Type.getType((String)enumInfo[0])) + '.' + enumInfo[1];
        }
        if (value instanceof AnnotationNode) {
            return ASMUtils.annotationToString((AnnotationNode)value);
        }
        if (value instanceof List) {
            List list = (List)value;
            if (list.size() == 1) {
                return ASMUtils.valueToString(list.get(0));
            }
            return '{' + list.stream().map(ASMUtils::valueToString).collect(Collectors.joining(", ")) + '}';
        }
        return value.toString();
    }

    public static boolean isPrimitive(Type type) {
        return type.getDescriptor().length() == 1;
    }

    public static MethodInsnNode getInvokeInstruction(ClassNode owner, MethodNode method) {
        boolean isInterface;
        boolean bl = isInterface = (owner.access & 0x200) != 0;
        int opcode = (method.access & 8) != 0 ? 184 : ((method.access & 2) != 0 ? 183 : (isInterface ? 185 : 182));
        return new MethodInsnNode(opcode, owner.name, method.name, method.desc, isInterface);
    }

    public static int getDummyOpcodeForType(Type type) {
        switch (type.getSort()) {
            case 1: 
            case 2: 
            case 3: 
            case 4: 
            case 5: {
                return 3;
            }
            case 6: {
                return 11;
            }
            case 7: {
                return 9;
            }
            case 8: {
                return 14;
            }
            case 9: 
            case 10: {
                return 1;
            }
        }
        throw new UnsupportedOperationException();
    }

    public static MethodInsnNode findInitNodeFor(Target target, TypeInsnNode newNode) {
        int start = target.indexOf((AbstractInsnNode)newNode);
        int depth = 0;
        ListIterator it = target.insns.iterator(start);
        while (it.hasNext()) {
            AbstractInsnNode insn = (AbstractInsnNode)it.next();
            if (insn instanceof TypeInsnNode && insn.getOpcode() == 187) {
                TypeInsnNode typeNode = (TypeInsnNode)insn;
                if (!typeNode.desc.equals(newNode.desc)) continue;
                ++depth;
                continue;
            }
            if (!(insn instanceof MethodInsnNode) || insn.getOpcode() != 183) continue;
            MethodInsnNode methodNode = (MethodInsnNode)insn;
            if (!"<init>".equals(methodNode.name) || !methodNode.owner.equals(newNode.desc) || --depth != 0) continue;
            return methodNode;
        }
        return null;
    }

    public static void ifElse(InsnList insns, int jumpToSecond, Runnable first, Runnable second) {
        ASMUtils.ifElse(insns, label -> insns.add((AbstractInsnNode)new JumpInsnNode(jumpToSecond, label)), first, second);
    }

    public static void ifElse(InsnList insns, Consumer<LabelNode> addJump, Runnable first, Runnable second) {
        LabelNode secondLabel = new LabelNode();
        LabelNode end = new LabelNode();
        addJump.accept(secondLabel);
        first.run();
        insns.add((AbstractInsnNode)new JumpInsnNode(167, end));
        insns.add((AbstractInsnNode)secondLabel);
        second.run();
        insns.add((AbstractInsnNode)end);
    }

    public static AnnotationNode getRepeatedMEAnnotation(MethodNode method, Class<? extends Annotation> single) {
        Class<? extends Annotation> container = single.getAnnotation(Repeatable.class).value();
        AnnotationNode repeated = ASMUtils.getInvisibleMEAnnotation(method, container);
        if (repeated != null) {
            return repeated;
        }
        AnnotationNode individual = ASMUtils.getInvisibleMEAnnotation(method, single);
        if (individual == null) {
            return null;
        }
        AnnotationNode result = new AnnotationNode(Type.getDescriptor(container));
        result.visit("value", (Object)individual);
        return result;
    }

    public static AnnotationNode getInvisibleMEAnnotation(MethodNode method, Class<? extends Annotation> annotation) {
        return ASMUtils.getMEAnnotation(method.invisibleAnnotations, Type.getInternalName(annotation));
    }

    private static AnnotationNode getMEAnnotation(List<AnnotationNode> annotations, String internalAnnotationName) {
        String annotationName = "." + StringUtils.substringAfterLast(internalAnnotationName, "/");
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            String binaryName = Type.getType((String)annotation.desc).getClassName();
            if (!MixinExtrasService.getInstance().isClassOwned(binaryName) || !binaryName.endsWith(annotationName)) continue;
            return annotation;
        }
        return null;
    }

    public static Type getConstantType(AbstractInsnNode insn) {
        if (insn instanceof TypeInsnNode) {
            return null;
        }
        return Bytecode.getConstantType(insn);
    }
}

