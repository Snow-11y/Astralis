package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.ExpressionService;
import com.llamalad7.mixinextras.expression.impl.flow.Boxing;
import com.llamalad7.mixinextras.expression.impl.flow.FlowContext;
import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.util.Bytecode;

public class ExpressionASMUtils {
    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final Type BOTTOM_TYPE = Type.getObjectType((String)"null");
    public static final Type INTLIKE_TYPE = Type.getObjectType((String)"int-like");
    public static final Handle LMF_HANDLE = new Handle(6, "java/lang/invoke/LambdaMetafactory", "metafactory", Bytecode.generateDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class), false);
    public static final Handle ALT_LMF_HANDLE = new Handle(6, "java/lang/invoke/LambdaMetafactory", "altMetafactory", Bytecode.generateDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class), false);

    public static Type getNewType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case 1: {
                return BOTTOM_TYPE;
            }
            case 2: 
            case 3: 
            case 4: 
            case 5: 
            case 6: 
            case 7: 
            case 8: 
            case 16: 
            case 17: {
                return INTLIKE_TYPE;
            }
            case 9: 
            case 10: {
                return Type.LONG_TYPE;
            }
            case 11: 
            case 12: 
            case 13: {
                return Type.FLOAT_TYPE;
            }
            case 14: 
            case 15: {
                return Type.DOUBLE_TYPE;
            }
            case 18: {
                Object cst = ((LdcInsnNode)insn).cst;
                if (cst instanceof Integer) {
                    return INTLIKE_TYPE;
                }
                if (cst instanceof Float) {
                    return Type.FLOAT_TYPE;
                }
                if (cst instanceof Long) {
                    return Type.LONG_TYPE;
                }
                if (cst instanceof Double) {
                    return Type.DOUBLE_TYPE;
                }
                if (cst instanceof String) {
                    return Type.getType(String.class);
                }
                if (cst instanceof Type) {
                    int sort = ((Type)cst).getSort();
                    if (sort == 10 || sort == 9) {
                        return Type.getType(Class.class);
                    }
                    if (sort == 11) {
                        return Type.getType(MethodType.class);
                    }
                }
                if (cst instanceof Handle) {
                    return Type.getType(MethodHandle.class);
                }
                throw new IllegalArgumentException("Illegal LDC constant " + cst);
            }
            case 178: {
                return Type.getType((String)((FieldInsnNode)insn).desc);
            }
            case 187: {
                return Type.getObjectType((String)((TypeInsnNode)insn).desc);
            }
        }
        throw ExpressionASMUtils.errorFor(insn);
    }

    public static Type getUnaryType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case 116: 
            case 132: 
            case 136: 
            case 139: 
            case 142: 
            case 190: {
                return Type.INT_TYPE;
            }
            case 145: {
                return Type.BYTE_TYPE;
            }
            case 146: {
                return Type.CHAR_TYPE;
            }
            case 147: {
                return Type.SHORT_TYPE;
            }
            case 118: 
            case 134: 
            case 137: 
            case 144: {
                return Type.FLOAT_TYPE;
            }
            case 117: 
            case 133: 
            case 140: 
            case 143: {
                return Type.LONG_TYPE;
            }
            case 119: 
            case 135: 
            case 138: 
            case 141: {
                return Type.DOUBLE_TYPE;
            }
            case 153: 
            case 154: 
            case 155: 
            case 156: 
            case 157: 
            case 158: 
            case 170: 
            case 171: 
            case 172: 
            case 173: 
            case 174: 
            case 175: 
            case 176: 
            case 179: 
            case 191: 
            case 194: 
            case 195: 
            case 198: 
            case 199: {
                return Type.VOID_TYPE;
            }
            case 180: {
                return Type.getType((String)((FieldInsnNode)insn).desc);
            }
            case 188: {
                switch (((IntInsnNode)insn).operand) {
                    case 4: {
                        return Type.getType((String)"[Z");
                    }
                    case 5: {
                        return Type.getType((String)"[C");
                    }
                    case 8: {
                        return Type.getType((String)"[B");
                    }
                    case 9: {
                        return Type.getType((String)"[S");
                    }
                    case 10: {
                        return Type.getType((String)"[I");
                    }
                    case 6: {
                        return Type.getType((String)"[F");
                    }
                    case 7: {
                        return Type.getType((String)"[D");
                    }
                    case 11: {
                        return Type.getType((String)"[J");
                    }
                }
                throw new Error("Invalid array type " + ((IntInsnNode)insn).operand);
            }
            case 189: {
                String desc = ((TypeInsnNode)insn).desc;
                return Type.getType((String)("[" + Type.getObjectType((String)desc)));
            }
            case 192: {
                String desc = ((TypeInsnNode)insn).desc;
                return Type.getObjectType((String)desc);
            }
            case 193: {
                return Type.BOOLEAN_TYPE;
            }
        }
        throw ExpressionASMUtils.errorFor(insn);
    }

    public static Type getBinaryType(AbstractInsnNode insn, Type left) {
        switch (insn.getOpcode()) {
            case 47: 
            case 97: 
            case 101: 
            case 105: 
            case 109: 
            case 113: 
            case 121: 
            case 123: 
            case 125: 
            case 127: 
            case 129: 
            case 131: {
                return Type.LONG_TYPE;
            }
            case 49: 
            case 99: 
            case 103: 
            case 107: 
            case 111: 
            case 115: {
                return Type.DOUBLE_TYPE;
            }
            case 46: 
            case 96: 
            case 100: 
            case 104: 
            case 108: 
            case 112: 
            case 120: 
            case 122: 
            case 124: 
            case 126: 
            case 128: 
            case 130: 
            case 148: 
            case 149: 
            case 150: 
            case 151: 
            case 152: {
                return Type.INT_TYPE;
            }
            case 48: 
            case 98: 
            case 102: 
            case 106: 
            case 110: 
            case 114: {
                return Type.FLOAT_TYPE;
            }
            case 52: {
                return Type.CHAR_TYPE;
            }
            case 53: {
                return Type.SHORT_TYPE;
            }
            case 159: 
            case 160: 
            case 161: 
            case 162: 
            case 163: 
            case 164: 
            case 165: 
            case 166: 
            case 181: {
                return Type.VOID_TYPE;
            }
            case 50: 
            case 51: {
                return ExpressionASMUtils.getInnerType(left);
            }
        }
        throw ExpressionASMUtils.errorFor(insn);
    }

    public static Type getNaryType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case 197: {
                return Type.getType((String)((MultiANewArrayInsnNode)insn).desc);
            }
            case 186: {
                return Type.getReturnType((String)((InvokeDynamicInsnNode)insn).desc);
            }
        }
        return Type.getReturnType((String)((MethodInsnNode)insn).desc);
    }

    private static Error errorFor(AbstractInsnNode insn) {
        return new AssertionError((Object)String.format("Could not compute type of %s! Please inform LlamaLad7!", Bytecode.describeNode(insn)));
    }

    public static Type getCommonSupertype(FlowContext ctx, Type type1, Type type2) {
        if (type1.equals((Object)type2) || type2.equals((Object)BOTTOM_TYPE)) {
            return type1;
        }
        if (type1.equals((Object)BOTTOM_TYPE)) {
            return type2;
        }
        boolean isIntLike1 = ExpressionASMUtils.isIntLike(type1);
        boolean isIntLike2 = ExpressionASMUtils.isIntLike(type2);
        if (isIntLike1 && isIntLike2) {
            return INTLIKE_TYPE;
        }
        if (isIntLike1 || isIntLike2) {
            return BOTTOM_TYPE;
        }
        if (type1.getSort() == 9 && type2.getSort() == 9) {
            int shared;
            Type smaller;
            int dim1 = type1.getDimensions();
            Type elem1 = type1.getElementType();
            int dim2 = type2.getDimensions();
            Type elem2 = type2.getElementType();
            if (dim1 == dim2) {
                Type commonSupertype;
                if (elem1.equals((Object)elem2)) {
                    commonSupertype = elem1;
                } else if (elem1.getSort() == 10 && elem2.getSort() == 10) {
                    commonSupertype = ExpressionASMUtils.getCommonSupertype(ctx, elem1, elem2);
                } else {
                    return ExpressionASMUtils.arrayType(OBJECT_TYPE, dim1 - 1);
                }
                return ExpressionASMUtils.arrayType(commonSupertype, dim1);
            }
            if (dim1 < dim2) {
                smaller = elem1;
                shared = dim1 - 1;
            } else {
                smaller = elem2;
                shared = dim2 - 1;
            }
            if (smaller.getSort() == 10) {
                ++shared;
            }
            return ExpressionASMUtils.arrayType(OBJECT_TYPE, shared);
        }
        if (type1.getSort() == 9 && type2.getSort() == 10 || type2.getSort() == 9 && type1.getSort() == 10) {
            return OBJECT_TYPE;
        }
        if (type1.getSort() != type2.getSort()) {
            return BOTTOM_TYPE;
        }
        return ExpressionService.getInstance().getCommonSuperClass(ctx, type1, type2);
    }

    public static Type getCommonIntType(FlowContext ctx, Type type1, Type type2) {
        Type unboxed1 = Boxing.getUnboxedType(type1);
        Type unboxed2 = Boxing.getUnboxedType(type2);
        return ExpressionASMUtils.getCommonSupertype(ctx, unboxed1 != null ? unboxed1 : type1, unboxed2 != null ? unboxed2 : type2);
    }

    public static boolean isIntLike(Type type) {
        switch (type.getSort()) {
            case 1: 
            case 2: 
            case 3: 
            case 4: 
            case 5: {
                return true;
            }
            case 10: {
                return type.equals((Object)INTLIKE_TYPE);
            }
        }
        return false;
    }

    private static Type arrayType(Type element, int dimensions) {
        return Type.getType((String)(StringUtils.repeat('[', dimensions) + element.getDescriptor()));
    }

    public static Type getInnerType(Type arrayType) {
        if (arrayType.equals((Object)BOTTOM_TYPE)) {
            return BOTTOM_TYPE;
        }
        return Type.getType((String)arrayType.getDescriptor().substring(1));
    }

    public static Type getNewArrayType(IntInsnNode newArray) {
        switch (newArray.operand) {
            case 4: {
                return Type.BOOLEAN_TYPE;
            }
            case 5: {
                return Type.CHAR_TYPE;
            }
            case 6: {
                return Type.FLOAT_TYPE;
            }
            case 7: {
                return Type.DOUBLE_TYPE;
            }
            case 8: {
                return Type.BYTE_TYPE;
            }
            case 9: {
                return Type.SHORT_TYPE;
            }
            case 10: {
                return Type.INT_TYPE;
            }
            case 11: {
                return Type.LONG_TYPE;
            }
        }
        return null;
    }

    public static Object getConstant(AbstractInsnNode insn) {
        if (insn.getOpcode() == 188) {
            return null;
        }
        if (insn instanceof TypeInsnNode) {
            return null;
        }
        return Bytecode.getConstant(insn);
    }

    public static AbstractInsnNode pushInt(int integer) {
        switch (integer) {
            case -1: {
                return new InsnNode(2);
            }
            case 0: {
                return new InsnNode(3);
            }
            case 1: {
                return new InsnNode(4);
            }
            case 2: {
                return new InsnNode(5);
            }
            case 3: {
                return new InsnNode(6);
            }
            case 4: {
                return new InsnNode(7);
            }
            case 5: {
                return new InsnNode(8);
            }
        }
        if (-128 <= integer && integer <= 127) {
            return new IntInsnNode(16, integer);
        }
        if (Short.MIN_VALUE <= integer && integer <= Short.MAX_VALUE) {
            return new IntInsnNode(17, integer);
        }
        return new LdcInsnNode((Object)integer);
    }

    public static Type getCastType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case 192: {
                return Type.getObjectType((String)((TypeInsnNode)insn).desc);
            }
            case 136: 
            case 139: 
            case 142: {
                return Type.INT_TYPE;
            }
            case 145: {
                return Type.BYTE_TYPE;
            }
            case 146: {
                return Type.CHAR_TYPE;
            }
            case 147: {
                return Type.SHORT_TYPE;
            }
            case 134: 
            case 137: 
            case 144: {
                return Type.FLOAT_TYPE;
            }
            case 133: 
            case 140: 
            case 143: {
                return Type.LONG_TYPE;
            }
            case 135: 
            case 138: 
            case 141: {
                return Type.DOUBLE_TYPE;
            }
        }
        return null;
    }
}

