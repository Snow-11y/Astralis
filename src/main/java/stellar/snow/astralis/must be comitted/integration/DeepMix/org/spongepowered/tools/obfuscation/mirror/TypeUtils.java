package org.spongepowered.tools.obfuscation.mirror;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.objectweb.asm.Type;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.SignaturePrinter;

public abstract class TypeUtils {
    private TypeUtils() {
    }

    public static PackageElement getPackage(TypeMirror type) {
        if (!(type instanceof DeclaredType)) {
            return null;
        }
        return TypeUtils.getPackage((TypeElement)((DeclaredType)type).asElement());
    }

    public static PackageElement getPackage(TypeElement type) {
        Element parent;
        for (parent = type.getEnclosingElement(); parent != null && !(parent instanceof PackageElement); parent = parent.getEnclosingElement()) {
        }
        return (PackageElement)parent;
    }

    public static String getElementType(Element element) {
        if (element instanceof TypeElement) {
            return "TypeElement";
        }
        if (element instanceof ExecutableElement) {
            return "ExecutableElement";
        }
        if (element instanceof VariableElement) {
            return "VariableElement";
        }
        if (element instanceof PackageElement) {
            return "PackageElement";
        }
        if (element instanceof TypeParameterElement) {
            return "TypeParameterElement";
        }
        return element.getClass().getSimpleName();
    }

    public static String stripGenerics(String type) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (int pos = 0; pos < type.length(); ++pos) {
            char c = type.charAt(pos);
            if (c == '<') {
                ++depth;
            }
            if (depth == 0) {
                sb.append(c);
                continue;
            }
            if (c != '>') continue;
            --depth;
        }
        return sb.toString();
    }

    public static String getName(VariableElement field) {
        return field != null ? field.getSimpleName().toString() : null;
    }

    public static String getName(ExecutableElement method) {
        return method != null ? method.getSimpleName().toString() : null;
    }

    public static String getJavaSignature(Element element) {
        if (element == null) {
            return "";
        }
        if (element instanceof ExecutableElement) {
            ExecutableElement method = (ExecutableElement)element;
            StringBuilder desc = new StringBuilder().append("(");
            boolean extra = false;
            for (VariableElement variableElement : method.getParameters()) {
                if (extra) {
                    desc.append(',');
                }
                desc.append(TypeUtils.getTypeName(variableElement.asType()));
                extra = true;
            }
            desc.append(')').append(TypeUtils.getTypeName(method.getReturnType()));
            return desc.toString();
        }
        return TypeUtils.getTypeName(element.asType());
    }

    public static String getJavaSignature(String descriptor) {
        if (!descriptor.contains("(")) {
            return SignaturePrinter.getTypeName(Type.getType((String)descriptor), false, true);
        }
        return new SignaturePrinter("", descriptor).setFullyQualified(true).toDescriptor();
    }

    public static String getSimpleName(TypeMirror type) {
        String name = TypeUtils.getTypeName(type);
        int pos = name.lastIndexOf(46);
        return pos > 0 ? name.substring(pos + 1) : name;
    }

    public static String getTypeName(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY: {
                return TypeUtils.getTypeName(((ArrayType)type).getComponentType()) + "[]";
            }
            case DECLARED: {
                return TypeUtils.getTypeName((DeclaredType)type);
            }
            case TYPEVAR: {
                return TypeUtils.getTypeName(TypeUtils.getUpperBound(type));
            }
            case ERROR: {
                return "java.lang.Object";
            }
        }
        return type.toString();
    }

    public static String getTypeName(DeclaredType type) {
        if (type == null) {
            return "java.lang.Object";
        }
        return TypeUtils.getInternalName((TypeElement)type.asElement()).replace('/', '.');
    }

    public static String getDescriptor(Element element) {
        if (element instanceof ExecutableElement) {
            return TypeUtils.getDescriptor((ExecutableElement)element);
        }
        if (element instanceof VariableElement) {
            return TypeUtils.getInternalName((VariableElement)element);
        }
        return TypeUtils.getInternalName(element.asType());
    }

    public static String getDescriptor(ExecutableElement method) {
        if (method == null) {
            return null;
        }
        StringBuilder signature = new StringBuilder();
        for (VariableElement variableElement : method.getParameters()) {
            signature.append(TypeUtils.getInternalName(variableElement));
        }
        String returnType = TypeUtils.getInternalName(method.getReturnType());
        return String.format("(%s)%s", signature, returnType);
    }

    public static String getInternalName(VariableElement field) {
        if (field == null) {
            return null;
        }
        return TypeUtils.getInternalName(field.asType());
    }

    public static String getInternalName(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY: {
                return "[" + TypeUtils.getInternalName(((ArrayType)type).getComponentType());
            }
            case DECLARED: {
                return "L" + TypeUtils.getInternalName((DeclaredType)type) + ";";
            }
            case TYPEVAR: {
                return "L" + TypeUtils.getInternalName(TypeUtils.getUpperBound(type)) + ";";
            }
            case BOOLEAN: {
                return "Z";
            }
            case BYTE: {
                return "B";
            }
            case CHAR: {
                return "C";
            }
            case DOUBLE: {
                return "D";
            }
            case FLOAT: {
                return "F";
            }
            case INT: {
                return "I";
            }
            case LONG: {
                return "J";
            }
            case SHORT: {
                return "S";
            }
            case VOID: {
                return "V";
            }
            case ERROR: {
                return "Ljava/lang/Object;";
            }
        }
        throw new IllegalArgumentException("Unable to parse type symbol " + type + " with " + (Object)((Object)type.getKind()) + " to equivalent bytecode type");
    }

    public static String getInternalName(DeclaredType type) {
        if (type == null) {
            return "java/lang/Object";
        }
        return TypeUtils.getInternalName((TypeElement)type.asElement());
    }

    public static String getInternalName(TypeElement element) {
        if (element == null) {
            return null;
        }
        StringBuilder reference = new StringBuilder();
        reference.append(element.getSimpleName());
        for (Element parent = element.getEnclosingElement(); parent != null; parent = parent.getEnclosingElement()) {
            if (parent instanceof TypeElement) {
                reference.insert(0, "$").insert(0, parent.getSimpleName());
                continue;
            }
            if (!(parent instanceof PackageElement)) continue;
            reference.insert(0, "/").insert(0, ((PackageElement)parent).getQualifiedName().toString().replace('.', '/'));
        }
        return reference.toString();
    }

    private static DeclaredType getUpperBound(TypeMirror type) {
        try {
            return TypeUtils.getUpperBound0(type, 5);
        }
        catch (IllegalStateException ex) {
            throw new IllegalArgumentException("Type symbol \"" + type + "\" is too complex", ex);
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unable to compute upper bound of type symbol " + type, ex);
        }
    }

    private static DeclaredType getUpperBound0(TypeMirror type, int depth) {
        if (depth == 0) {
            throw new IllegalStateException("Generic symbol \"" + type + "\" is too complex, exceeded " + 5 + " iterations attempting to determine upper bound");
        }
        if (type instanceof IntersectionType) {
            TypeMirror first = ((IntersectionType)type).getBounds().get(0);
            return TypeUtils.getUpperBound0(first, --depth);
        }
        if (type instanceof DeclaredType) {
            return (DeclaredType)type;
        }
        if (type instanceof TypeVariable) {
            try {
                TypeMirror upper = ((TypeVariable)type).getUpperBound();
                return TypeUtils.getUpperBound0(upper, --depth);
            }
            catch (IllegalStateException ex) {
                throw ex;
            }
            catch (IllegalArgumentException ex) {
                throw ex;
            }
            catch (Exception ex) {
                throw new IllegalArgumentException("Unable to compute upper bound of type symbol " + type);
            }
        }
        return null;
    }

    private static String describeGenericBound(TypeMirror type) {
        if (type instanceof TypeVariable) {
            TypeMirror upperBound;
            StringBuilder description = new StringBuilder("<");
            TypeVariable typeVar = (TypeVariable)type;
            description.append(typeVar.toString());
            TypeMirror lowerBound = typeVar.getLowerBound();
            if (lowerBound.getKind() != TypeKind.NULL) {
                description.append(" super ").append(lowerBound);
            }
            if ((upperBound = typeVar.getUpperBound()).getKind() != TypeKind.NULL) {
                description.append(" extends ").append(upperBound);
            }
            return description.append(">").toString();
        }
        return type.toString();
    }

    public static boolean isAssignable(ProcessingEnvironment processingEnv, TypeMirror targetType, TypeMirror superClass) {
        boolean assignable = processingEnv.getTypeUtils().isAssignable(targetType, superClass);
        if (!assignable && targetType instanceof DeclaredType && superClass instanceof DeclaredType) {
            TypeMirror rawTargetType = TypeUtils.toRawType(processingEnv, (DeclaredType)targetType);
            TypeMirror rawSuperType = TypeUtils.toRawType(processingEnv, (DeclaredType)superClass);
            return processingEnv.getTypeUtils().isAssignable(rawTargetType, rawSuperType);
        }
        return assignable;
    }

    public static EquivalencyResult isEquivalentType(ProcessingEnvironment processingEnv, TypeMirror t1, TypeMirror t2) {
        if (t1 == null || t2 == null) {
            return EquivalencyResult.notEquivalent("Invalid types supplied: %s, %s", t1, t2);
        }
        if (processingEnv.getTypeUtils().isSameType(t1, t2)) {
            return EquivalencyResult.EQUIVALENT;
        }
        if (t1 instanceof TypeVariable && t2 instanceof TypeVariable) {
            t1 = TypeUtils.getUpperBound(t1);
            t2 = TypeUtils.getUpperBound(t2);
            if (processingEnv.getTypeUtils().isSameType(t1, t2)) {
                return EquivalencyResult.EQUIVALENT;
            }
        }
        if (t1 instanceof DeclaredType && t2 instanceof DeclaredType) {
            DeclaredType dtT1 = (DeclaredType)t1;
            DeclaredType dtT2 = (DeclaredType)t2;
            TypeMirror rawT1 = TypeUtils.toRawType(processingEnv, dtT1);
            TypeMirror rawT2 = TypeUtils.toRawType(processingEnv, dtT2);
            if (!processingEnv.getTypeUtils().isSameType(rawT1, rawT2)) {
                return EquivalencyResult.notEquivalent("Base types %s and %s are not compatible", rawT1, rawT2);
            }
            List<? extends TypeMirror> argsT1 = dtT1.getTypeArguments();
            List<? extends TypeMirror> argsT2 = dtT2.getTypeArguments();
            if (argsT1.size() != argsT2.size()) {
                if (argsT1.size() == 0) {
                    return EquivalencyResult.equivalentButRaw(1);
                }
                if (argsT2.size() == 0) {
                    return EquivalencyResult.equivalentButRaw(2);
                }
                return EquivalencyResult.notEquivalent("Mismatched generic argument counts %s<[%d]> and %s<[%d]>", rawT1, argsT1.size(), rawT2, argsT2.size());
            }
            for (int arg = 0; arg < argsT1.size(); ++arg) {
                TypeMirror argT1 = argsT1.get(arg);
                TypeMirror argT2 = argsT2.get(arg);
                if (TypeUtils.isEquivalentType((ProcessingEnvironment)processingEnv, (TypeMirror)argT1, (TypeMirror)argT2).type == Equivalency.EQUIVALENT) continue;
                return EquivalencyResult.boundsMismatch("Generic bounds mismatch between %s and %s", TypeUtils.describeGenericBound(argT1), TypeUtils.describeGenericBound(argT2));
            }
            return EquivalencyResult.EQUIVALENT;
        }
        return EquivalencyResult.notEquivalent("%s and %s do not match", t1, t2);
    }

    private static TypeMirror toRawType(ProcessingEnvironment processingEnv, DeclaredType targetType) {
        if (targetType.getKind() == TypeKind.INTERSECTION) {
            return targetType;
        }
        Name qualifiedName = ((TypeElement)targetType.asElement()).getQualifiedName();
        TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(qualifiedName);
        return typeElement != null ? typeElement.asType() : targetType;
    }

    public static Bytecode.Visibility getVisibility(Element element) {
        if (element == null) {
            return null;
        }
        for (Modifier modifier : element.getModifiers()) {
            switch (modifier) {
                case PUBLIC: {
                    return Bytecode.Visibility.PUBLIC;
                }
                case PROTECTED: {
                    return Bytecode.Visibility.PROTECTED;
                }
                case PRIVATE: {
                    return Bytecode.Visibility.PRIVATE;
                }
            }
        }
        return Bytecode.Visibility.PACKAGE;
    }

    public static class EquivalencyResult {
        static final EquivalencyResult EQUIVALENT = new EquivalencyResult(Equivalency.EQUIVALENT, "", 0);
        public final Equivalency type;
        public final String detail;
        public final int rawType;

        EquivalencyResult(Equivalency type, String detail, int rawType) {
            this.type = type;
            this.detail = detail;
            this.rawType = rawType;
        }

        public String toString() {
            return this.detail;
        }

        static EquivalencyResult notEquivalent(String format, Object ... args) {
            return new EquivalencyResult(Equivalency.NOT_EQUIVALENT, String.format(format, args), 0);
        }

        static EquivalencyResult boundsMismatch(String format, Object ... args) {
            return new EquivalencyResult(Equivalency.BOUNDS_MISMATCH, String.format(format, args), 0);
        }

        static EquivalencyResult equivalentButRaw(int rawType) {
            return new EquivalencyResult(Equivalency.EQUIVALENT_BUT_RAW, String.format("Type %d is raw", rawType), rawType);
        }
    }

    public static enum Equivalency {
        NOT_EQUIVALENT,
        EQUIVALENT_BUT_RAW,
        BOUNDS_MISMATCH,
        EQUIVALENT;

    }
}

