package org.spongepowered.asm.util.asm;

import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

public class MixinVerifier
extends SimpleVerifier {
    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    public MixinVerifier(int api, Type currentClass, Type currentSuperClass, List<Type> currentClassInterfaces, boolean isInterface) {
        super(api, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
    }

    protected boolean isInterface(Type type) {
        if (type.getSort() != 10) {
            return false;
        }
        return ClassInfo.forType(type, ClassInfo.TypeLookup.DECLARED_TYPE).isInterface();
    }

    protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        Type expectedType = expected.getType();
        Type type = value.getType();
        switch (expectedType.getSort()) {
            case 5: 
            case 6: 
            case 7: 
            case 8: {
                return type.equals((Object)expectedType);
            }
            case 9: 
            case 10: {
                if ("Lnull;".equals(type.getDescriptor())) {
                    return true;
                }
                if (type.getSort() == 10 || type.getSort() == 9) {
                    if (this.isAssignableFrom(expectedType, type)) {
                        return true;
                    }
                    if (expectedType.getSort() == 9) {
                        if (type.getSort() != 9) {
                            return false;
                        }
                        int dim = expectedType.getDimensions();
                        expectedType = expectedType.getElementType();
                        if (dim > type.getDimensions() || expectedType.getSort() != 10) {
                            return false;
                        }
                        type = Type.getType((String)type.getDescriptor().substring(dim));
                    }
                    if (this.isInterface(expectedType)) {
                        return type.getSort() >= 9;
                    }
                    return false;
                }
                return false;
            }
        }
        throw new AssertionError();
    }

    protected boolean isAssignableFrom(Type type1, Type type2) {
        return type1.equals((Object)MixinVerifier.getCommonSupertype(type1, type2));
    }

    public BasicValue merge(BasicValue value1, BasicValue value2) {
        if (value1.equals((Object)value2)) {
            return value1;
        }
        if (value1.equals((Object)BasicValue.UNINITIALIZED_VALUE) || value2.equals((Object)BasicValue.UNINITIALIZED_VALUE)) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        Type supertype = MixinVerifier.getCommonSupertype(value1.getType(), value2.getType());
        return this.newValue(supertype);
    }

    private static Type getCommonSupertype(Type type1, Type type2) {
        if (type1.equals((Object)type2) || "Lnull;".equals(type2.getDescriptor())) {
            return type1;
        }
        if ("Lnull;".equals(type1.getDescriptor())) {
            return type2;
        }
        if (type1.getSort() < 9 || type2.getSort() < 9) {
            return null;
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
                    commonSupertype = MixinVerifier.getCommonSupertype(elem1, elem2);
                } else {
                    return MixinVerifier.arrayType(OBJECT_TYPE, dim1 - 1);
                }
                return MixinVerifier.arrayType(commonSupertype, dim1);
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
            return MixinVerifier.arrayType(OBJECT_TYPE, shared);
        }
        if (type1.getSort() == 9 && type2.getSort() == 10 || type2.getSort() == 9 && type1.getSort() == 10) {
            return OBJECT_TYPE;
        }
        return ClassInfo.getCommonSuperClass(type1, type2).getType();
    }

    private static Type arrayType(Type type, int dimensions) {
        if (dimensions == 0) {
            return type;
        }
        StringBuilder descriptor = new StringBuilder();
        for (int i = 0; i < dimensions; ++i) {
            descriptor.append('[');
        }
        descriptor.append(type.getDescriptor());
        return Type.getType((String)descriptor.toString());
    }

    protected Class<?> getClass(Type type) {
        throw new UnsupportedOperationException(String.format("Live-loading of %s attempted by MixinVerifier! This should never happen!", type.getClassName()));
    }
}

