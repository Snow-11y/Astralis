package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

public class InjectorUtils {
    public static boolean isVirtualRedirect(InjectionNodes.InjectionNode node) {
        return node.isReplaced() && node.hasDecoration("redirector") && node.getCurrentTarget().getOpcode() != 184;
    }

    public static boolean isDynamicInstanceofRedirect(InjectionNodes.InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        return originalTarget.getOpcode() == 193 && currentTarget instanceof MethodInsnNode && Type.getReturnType((String)((MethodInsnNode)currentTarget).desc).equals((Object)Type.getType(Class.class));
    }

    public static void checkForDupedNews(Map<Target, List<InjectionNodes.InjectionNode>> targets) {
        for (Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : targets.entrySet()) {
            for (InjectionNodes.InjectionNode node : entry.getValue()) {
                AbstractInsnNode currentTarget = node.getCurrentTarget();
                if (currentTarget.getOpcode() != 187 || currentTarget.getNext().getOpcode() != 89) continue;
                node.decorate("mixinextras_newIsDuped", true);
            }
        }
    }

    public static boolean isDupedNew(InjectionNodes.InjectionNode node) {
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        return currentTarget != null && currentTarget.getOpcode() == 187 && node.hasDecoration("mixinextras_newIsDuped");
    }

    public static boolean isDupedFactoryRedirect(InjectionNodes.InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        return node.isReplaced() && originalTarget.getOpcode() == 187 && !node.hasDecoration("mixinextras_wrappedOperation") && node.hasDecoration("mixinextras_newIsDuped");
    }

    public static void checkForImmediatePops(Map<Target, List<InjectionNodes.InjectionNode>> targets) {
        for (List<InjectionNodes.InjectionNode> nodeList : targets.values()) {
            for (InjectionNodes.InjectionNode node : nodeList) {
                Type returnType;
                AbstractInsnNode currentTarget = node.getCurrentTarget();
                if (!(currentTarget instanceof MethodInsnNode) || !InjectorUtils.isTypePoppedByInstruction(returnType = Type.getReturnType((String)((MethodInsnNode)currentTarget).desc), currentTarget.getNext())) continue;
                node.decorate("mixinextras_operationIsImmediatelyPopped", true);
            }
        }
    }

    private static boolean isTypePoppedByInstruction(Type type, AbstractInsnNode insn) {
        switch (type.getSize()) {
            case 2: {
                return insn.getOpcode() == 88;
            }
            case 1: {
                return insn.getOpcode() == 87;
            }
        }
        return false;
    }

    public static LocalVariableDiscriminator.Context getOrCreateLocalContext(Target target, InjectionNodes.InjectionNode node, InjectionInfo info, Type targetType, boolean isArgsOnly) {
        String decorationKey = InjectorUtils.getLocalContextKey(targetType, isArgsOnly);
        if (node.hasDecoration(decorationKey)) {
            return (LocalVariableDiscriminator.Context)node.getDecoration(decorationKey);
        }
        LocalVariableDiscriminator.Context context = CompatibilityHelper.makeLvtContext(info, targetType, isArgsOnly, target, node.getCurrentTarget());
        node.decorate(decorationKey, context);
        return context;
    }

    private static String getLocalContextKey(Type targetType, boolean isArgsOnly) {
        return String.format("mixinextras_persistent_localContext(%s,%s)", targetType, isArgsOnly ? "argsOnly" : "fullFrame");
    }

    public static void printLocals(Target target, AbstractInsnNode node, LocalVariableDiscriminator.Context context, LocalVariableDiscriminator discriminator, Type targetType, boolean isArgsOnly) {
        int baseArgIndex = target.isStatic ? 0 : 1;
        new PrettyPrinter().kvWidth(20).kv("Target Class", target.classNode.name.replace('/', '.')).kv("Target Method", target.method.name).kv("Capture Type", SignaturePrinter.getTypeName(targetType, false)).kv("Instruction", "[%d] %s %s", target.insns.indexOf(node), node.getClass().getSimpleName(), Bytecode.getOpcodeName(node.getOpcode())).hr().kv("Match mode", InjectorUtils.isImplicit(discriminator, baseArgIndex) ? "IMPLICIT (match single)" : "EXPLICIT (match by criteria)").kv("Match ordinal", discriminator.getOrdinal() < 0 ? "any" : Integer.valueOf(discriminator.getOrdinal())).kv("Match index", discriminator.getIndex() < baseArgIndex ? "any" : Integer.valueOf(discriminator.getIndex())).kv("Match name(s)", discriminator.hasNames() ? discriminator.getNames() : "any").kv("Args only", isArgsOnly).hr().add(context).print(System.err);
    }

    private static boolean isImplicit(LocalVariableDiscriminator discriminator, int baseArgIndex) {
        return discriminator.getOrdinal() < 0 && discriminator.getIndex() < baseArgIndex && discriminator.getNames().isEmpty();
    }

    public static void decorateInjectorSpecific(InjectionNodes.InjectionNode node, InjectionInfo info, String key, Object value) {
        if (!node.hasDecoration(key)) {
            node.decorate(key, new HashMap());
        }
        Map inner = (Map)node.getDecoration(key);
        inner.put(info, value);
    }

    public static <T> T getInjectorSpecificDecoration(InjectionNodes.InjectionNode node, InjectionInfo info, String key) {
        Map map = (Map)node.getDecoration(key);
        if (map == null) {
            return null;
        }
        return (T)map.get(info);
    }

    public static boolean hasInjectorSpecificDecoration(InjectionNodes.InjectionNode node, InjectionInfo info, String key) {
        Map map = (Map)node.getDecoration(key);
        if (map == null) {
            return false;
        }
        return map.containsKey(info);
    }

    public static void coerceReturnType(Injector.InjectorData data, InsnList insns, Type expectedReturnType) {
        if (data.coerceReturnType && expectedReturnType.getSort() >= 9) {
            insns.add((AbstractInsnNode)new TypeInsnNode(192, expectedReturnType.getInternalName()));
        }
    }

    public static AbstractInsnNode findCoerce(InjectionNodes.InjectionNode target, Type expectedType) {
        if (!target.isReplaced() || InjectorUtils.isDynamicInstanceofRedirect(target)) {
            return null;
        }
        AbstractInsnNode currentTarget = target.getCurrentTarget();
        if (!(currentTarget instanceof MethodInsnNode)) {
            return null;
        }
        MethodInsnNode handlerCall = (MethodInsnNode)currentTarget;
        if (ASMUtils.isPrimitive(expectedType) || Type.getReturnType((String)handlerCall.desc).equals((Object)expectedType)) {
            return null;
        }
        if (handlerCall.getNext().getOpcode() == 192) {
            TypeInsnNode cast = (TypeInsnNode)handlerCall.getNext();
            if (cast.desc.equals(expectedType.getInternalName())) {
                return cast;
            }
        }
        throw new AssertionError((Object)String.format("Could not find @Coerce CHECKCAST instruction! Expected '%s' but got '%s'! Please inform LlamaLad7!", "[CHECKCAST] " + expectedType.getInternalName(), Bytecode.describeNode(handlerCall.getNext())));
    }
}

