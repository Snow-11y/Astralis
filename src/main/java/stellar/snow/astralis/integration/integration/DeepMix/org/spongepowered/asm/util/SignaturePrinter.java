package org.spongepowered.asm.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.include.com.google.common.base.Strings;

public class SignaturePrinter {
    private final String name;
    private final Type returnType;
    private final Type[] argTypes;
    private final String[] argNames;
    private String modifiers = "private void";
    private boolean fullyQualified;

    public SignaturePrinter(MethodNode method) {
        this(method.name, Type.VOID_TYPE, Type.getArgumentTypes((String)method.desc));
        this.setModifiers(method);
    }

    public SignaturePrinter(MethodNode method, String[] argNames) {
        this(method.name, Type.VOID_TYPE, Type.getArgumentTypes((String)method.desc), argNames);
        this.setModifiers(method);
    }

    public SignaturePrinter(ITargetSelectorByName member) {
        this(member.getName(), member.getDesc());
    }

    public SignaturePrinter(String name, String desc) {
        this(name, Type.getReturnType((String)desc), Type.getArgumentTypes((String)desc));
    }

    public SignaturePrinter(Type[] args) {
        this(null, null, args);
    }

    public SignaturePrinter(Type returnType, Type[] args) {
        this(null, returnType, args);
    }

    public SignaturePrinter(String name, Type returnType, Type[] args) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = new Type[args.length];
        this.argNames = new String[args.length];
        int v = 0;
        for (int l = 0; l < args.length; ++l) {
            if (args[l] == null) continue;
            this.argTypes[l] = args[l];
            this.argNames[l] = "var" + v++;
        }
    }

    public SignaturePrinter(String name, Type returnType, LocalVariableNode[] args) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = new Type[args.length];
        this.argNames = new String[args.length];
        for (int l = 0; l < args.length; ++l) {
            if (args[l] == null) continue;
            this.argTypes[l] = Type.getType((String)args[l].desc);
            this.argNames[l] = args[l].name;
        }
    }

    public SignaturePrinter(String name, Type returnType, Type[] argTypes, String[] argNames) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = argTypes;
        this.argNames = argNames;
    }

    public String getFormattedArgs() {
        return this.appendArgs(new StringBuilder(), true, true).toString();
    }

    public String getReturnType() {
        return SignaturePrinter.getTypeName(this.returnType, false, this.fullyQualified);
    }

    public void setModifiers(MethodNode method) {
        String staticType;
        String returnType = SignaturePrinter.getTypeName(Type.getReturnType((String)method.desc), false, this.fullyQualified);
        String string = staticType = (method.access & 8) != 0 ? "static " : "";
        if ((method.access & 1) != 0) {
            this.setModifiers("public " + staticType + returnType);
        } else if ((method.access & 4) != 0) {
            this.setModifiers("protected " + staticType + returnType);
        } else if ((method.access & 2) != 0) {
            this.setModifiers("private " + staticType + returnType);
        } else {
            this.setModifiers(staticType + returnType);
        }
    }

    public SignaturePrinter setModifiers(String modifiers) {
        this.modifiers = modifiers.replace("${returnType}", this.getReturnType());
        return this;
    }

    public SignaturePrinter setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
        return this;
    }

    public boolean isFullyQualified() {
        return this.fullyQualified;
    }

    public String toString() {
        String name = this.name != null ? this.name : "method";
        return this.appendArgs(new StringBuilder().append(this.modifiers).append(" ").append(name), false, true).toString();
    }

    public String toDescriptor() {
        StringBuilder args = this.appendArgs(new StringBuilder(), true, false);
        return args.append(SignaturePrinter.getTypeName(this.returnType, false, this.fullyQualified)).toString();
    }

    private StringBuilder appendArgs(StringBuilder sb, boolean typesOnly, boolean pretty) {
        sb.append('(');
        for (int var = 0; var < this.argTypes.length; ++var) {
            if (this.argTypes[var] == null) continue;
            if (var > 0) {
                sb.append(',');
                if (pretty) {
                    sb.append(' ');
                }
            }
            try {
                String name = typesOnly ? null : (var < this.argNames.length && !Strings.isNullOrEmpty(this.argNames[var]) ? this.argNames[var] : "unnamed" + var);
                this.appendType(sb, this.argTypes[var], name);
                continue;
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return sb.append(")");
    }

    private StringBuilder appendType(StringBuilder sb, Type type, String name) {
        switch (type.getSort()) {
            case 9: {
                return SignaturePrinter.appendArraySuffix(this.appendType(sb, SignaturePrinter.getElementType(type), name), type);
            }
            case 10: {
                return this.appendType(sb, SignaturePrinter.getClassName(type), name);
            }
        }
        sb.append(SignaturePrinter.getTypeName(type, false, this.fullyQualified));
        if (name != null) {
            sb.append(' ').append(name);
        }
        return sb;
    }

    private StringBuilder appendType(StringBuilder sb, String typeName, String name) {
        if (!this.fullyQualified) {
            typeName = typeName.substring(typeName.lastIndexOf(46) + 1);
        }
        sb.append(typeName);
        if (typeName.endsWith("CallbackInfoReturnable")) {
            sb.append('<').append(SignaturePrinter.getTypeName(this.returnType, true, this.fullyQualified)).append('>');
        }
        if (name != null) {
            sb.append(' ').append(name);
        }
        return sb;
    }

    public static String getTypeName(Type type) {
        return SignaturePrinter.getTypeName(type, false, true);
    }

    public static String getTypeName(Type type, boolean box) {
        return SignaturePrinter.getTypeName(type, box, false);
    }

    public static String getTypeName(Type type, boolean box, boolean fullyQualified) {
        if (type == null) {
            return "{null?}";
        }
        switch (type.getSort()) {
            case 0: {
                return box ? "Void" : "void";
            }
            case 1: {
                return box ? "Boolean" : "boolean";
            }
            case 2: {
                return box ? "Character" : "char";
            }
            case 3: {
                return box ? "Byte" : "byte";
            }
            case 4: {
                return box ? "Short" : "short";
            }
            case 5: {
                return box ? "Integer" : "int";
            }
            case 6: {
                return box ? "Float" : "float";
            }
            case 7: {
                return box ? "Long" : "long";
            }
            case 8: {
                return box ? "Double" : "double";
            }
            case 9: {
                return SignaturePrinter.getTypeName(SignaturePrinter.getElementType(type), box, fullyQualified) + SignaturePrinter.arraySuffix(type);
            }
            case 10: {
                String typeName = SignaturePrinter.getClassName(type);
                if (!fullyQualified) {
                    typeName = typeName.substring(typeName.lastIndexOf(46) + 1);
                }
                return typeName;
            }
        }
        return "Object";
    }

    private static Type getElementType(Type type) {
        try {
            return type.getElementType();
        }
        catch (Exception ex) {
            return Type.getObjectType((String)"InvalidType");
        }
    }

    private static String getClassName(Type type) {
        try {
            return type.getClassName();
        }
        catch (Exception ex) {
            return "InvalidType";
        }
    }

    private static String arraySuffix(Type type) {
        return Strings.repeat("[]", type.getDimensions());
    }

    private static StringBuilder appendArraySuffix(StringBuilder sb, Type type) {
        for (int i = 0; i < type.getDimensions(); ++i) {
            sb.append("[]");
        }
        return sb;
    }
}

