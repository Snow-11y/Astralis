package org.spongepowered.asm.mixin.injection.invoke.arg;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.transformer.SyntheticClassInfo;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.IConsumer;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.asm.MethodVisitorEx;

public final class ArgsClassGenerator
implements IClassGenerator {
    public static final String ARGS_NAME = Args.class.getName();
    public static final String ARGS_REF = ARGS_NAME.replace('.', '/');
    public static final String GETTER_PREFIX = "$";
    public static final String SYNTHETIC_PACKAGE = "org.spongepowered.asm.synthetic.args";
    private static final ILogger logger = MixinService.getService().getLogger("mixin");
    private final IConsumer<ISyntheticClassInfo> registry;
    private int nextIndex = 1;
    private final Map<String, ArgsClassInfo> descToClass = new HashMap<String, ArgsClassInfo>();
    private final Map<String, ArgsClassInfo> nameToClass = new HashMap<String, ArgsClassInfo>();

    public ArgsClassGenerator(IConsumer<ISyntheticClassInfo> registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "args";
    }

    public ISyntheticClassInfo getArgsClass(String desc, IMixinInfo mixin) {
        String voidDesc = Bytecode.changeDescriptorReturnType(desc, "V");
        ArgsClassInfo info = this.descToClass.get(voidDesc);
        if (info == null) {
            String name = String.format("%s%d", "org.spongepowered.asm.synthetic.args.Args$", this.nextIndex++);
            logger.debug("ArgsClassGenerator assigning {} for descriptor {}", name, voidDesc);
            info = new ArgsClassInfo(mixin, name, voidDesc);
            this.descToClass.put(voidDesc, info);
            this.nameToClass.put(name, info);
            this.registry.accept(info);
        }
        return info;
    }

    @Override
    public boolean generate(String name, ClassNode classNode) {
        ArgsClassInfo info = this.nameToClass.get(name);
        if (info == null) {
            return false;
        }
        if (info.loaded > 0) {
            logger.debug("ArgsClassGenerator is re-generating {}, already did this {} times!", name, info.loaded);
        }
        ClassNode visitor = classNode;
        if (MixinEnvironment.getCurrentEnvironment().getOption(MixinEnvironment.Option.DEBUG_VERIFY)) {
            visitor = new CheckClassAdapter((ClassVisitor)classNode);
        }
        visitor.visit(50, 4129, info.getName(), null, ARGS_REF, null);
        visitor.visitSource(name.substring(name.lastIndexOf(46) + 1) + ".java", null);
        this.generateCtor(info, (ClassVisitor)visitor);
        this.generateToString(info, (ClassVisitor)visitor);
        this.generateFactory(info, (ClassVisitor)visitor);
        this.generateSetters(info, (ClassVisitor)visitor);
        this.generateGetters(info, (ClassVisitor)visitor);
        visitor.visitEnd();
        ++info.loaded;
        return true;
    }

    private void generateCtor(ArgsClassInfo info, ClassVisitor writer) {
        MethodVisitor ctor = writer.visitMethod(2, "<init>", "([Ljava/lang/Object;)V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(25, 0);
        ctor.visitVarInsn(25, 1);
        ctor.visitMethodInsn(183, ARGS_REF, "<init>", "([Ljava/lang/Object;)V", false);
        ctor.visitInsn(177);
        ctor.visitMaxs(2, 2);
        ctor.visitEnd();
    }

    private void generateToString(ArgsClassInfo info, ClassVisitor writer) {
        MethodVisitor toString = writer.visitMethod(1, "toString", "()Ljava/lang/String;", null, null);
        toString.visitCode();
        toString.visitLdcInsn((Object)("Args" + info.getSignature()));
        toString.visitInsn(176);
        toString.visitMaxs(1, 1);
        toString.visitEnd();
    }

    private void generateFactory(ArgsClassInfo info, ClassVisitor writer) {
        String ref = info.getName();
        String factoryDesc = Bytecode.changeDescriptorReturnType(info.desc, "L" + ref + ";");
        MethodVisitorEx of = new MethodVisitorEx(writer.visitMethod(9, "of", factoryDesc, null, null));
        of.visitCode();
        of.visitTypeInsn(187, ref);
        of.visitInsn(89);
        of.visitConstant((byte)info.args.length);
        of.visitTypeInsn(189, "java/lang/Object");
        int argIndex = 0;
        for (byte index = 0; index < info.args.length; index = (byte)(index + 1)) {
            Type arg = info.args[index];
            of.visitInsn(89);
            of.visitConstant(index);
            of.visitVarInsn(arg.getOpcode(21), argIndex);
            ArgsClassGenerator.box(of, arg);
            of.visitInsn(83);
            argIndex = (byte)(argIndex + arg.getSize());
        }
        of.visitMethodInsn(183, ref, "<init>", "([Ljava/lang/Object;)V", false);
        of.visitInsn(176);
        of.visitMaxs(6, Bytecode.getArgsSize(info.args));
        of.visitEnd();
    }

    private void generateGetters(ArgsClassInfo info, ClassVisitor writer) {
        byte argIndex = 0;
        for (Type arg : info.args) {
            String name = GETTER_PREFIX + argIndex;
            String sig = "()" + arg.getDescriptor();
            MethodVisitorEx get = new MethodVisitorEx(writer.visitMethod(1, name, sig, null, null));
            get.visitCode();
            get.visitVarInsn(25, 0);
            get.visitFieldInsn(180, info.getName(), "values", "[Ljava/lang/Object;");
            get.visitConstant(argIndex);
            get.visitInsn(50);
            ArgsClassGenerator.unbox(get, arg);
            get.visitInsn(arg.getOpcode(172));
            get.visitMaxs(2, 1);
            get.visitEnd();
            argIndex = (byte)(argIndex + 1);
        }
    }

    private void generateSetters(ArgsClassInfo info, ClassVisitor writer) {
        this.generateIndexedSetter(info, writer);
        this.generateMultiSetter(info, writer);
    }

    private void generateIndexedSetter(ArgsClassInfo info, ClassVisitor writer) {
        int index;
        MethodVisitorEx set = new MethodVisitorEx(writer.visitMethod(1, "set", "(ILjava/lang/Object;)V", null, null));
        set.visitCode();
        Label store = new Label();
        Label checkNull = new Label();
        Label[] labels = new Label[info.args.length];
        for (int label = 0; label < labels.length; ++label) {
            labels[label] = new Label();
        }
        set.visitVarInsn(25, 0);
        set.visitFieldInsn(180, info.getName(), "values", "[Ljava/lang/Object;");
        for (index = 0; index < info.args.length; index = (byte)(index + 1)) {
            set.visitVarInsn(21, 1);
            set.visitConstant((byte)index);
            set.visitJumpInsn(159, labels[index]);
        }
        ArgsClassGenerator.throwAIOOBE(set, 1);
        for (index = 0; index < info.args.length; ++index) {
            String boxingType = Bytecode.getBoxingType(info.args[index]);
            set.visitLabel(labels[index]);
            set.visitVarInsn(21, 1);
            set.visitVarInsn(25, 2);
            set.visitTypeInsn(192, boxingType != null ? boxingType : info.args[index].getInternalName());
            set.visitJumpInsn(167, boxingType != null ? checkNull : store);
        }
        set.visitLabel(checkNull);
        set.visitInsn(89);
        set.visitJumpInsn(199, store);
        ArgsClassGenerator.throwNPE(set, "Argument with primitive type cannot be set to NULL");
        set.visitLabel(store);
        set.visitInsn(83);
        set.visitInsn(177);
        set.visitMaxs(6, 3);
        set.visitEnd();
    }

    private void generateMultiSetter(ArgsClassInfo info, ClassVisitor writer) {
        MethodVisitorEx set = new MethodVisitorEx(writer.visitMethod(1, "setAll", "([Ljava/lang/Object;)V", null, null));
        set.visitCode();
        Label lengthOk = new Label();
        Label nullPrimitive = new Label();
        int maxStack = 6;
        set.visitVarInsn(25, 1);
        set.visitInsn(190);
        set.visitInsn(89);
        set.visitConstant((byte)info.args.length);
        set.visitJumpInsn(159, lengthOk);
        set.visitTypeInsn(187, "org/spongepowered/asm/mixin/injection/invoke/arg/ArgumentCountException");
        set.visitInsn(89);
        set.visitInsn(93);
        set.visitInsn(88);
        set.visitConstant((byte)info.args.length);
        set.visitLdcInsn(info.getSignature());
        set.visitMethodInsn(183, "org/spongepowered/asm/mixin/injection/invoke/arg/ArgumentCountException", "<init>", "(IILjava/lang/String;)V", false);
        set.visitInsn(191);
        set.visitLabel(lengthOk);
        set.visitInsn(87);
        set.visitVarInsn(25, 0);
        set.visitFieldInsn(180, info.getName(), "values", "[Ljava/lang/Object;");
        for (byte index = 0; index < info.args.length; index = (byte)(index + 1)) {
            set.visitInsn(89);
            set.visitConstant(index);
            set.visitVarInsn(25, 1);
            set.visitConstant(index);
            set.visitInsn(50);
            String boxingType = Bytecode.getBoxingType(info.args[index]);
            set.visitTypeInsn(192, boxingType != null ? boxingType : info.args[index].getInternalName());
            if (boxingType != null) {
                set.visitInsn(89);
                set.visitJumpInsn(198, nullPrimitive);
                maxStack = 7;
            }
            set.visitInsn(83);
        }
        set.visitInsn(177);
        set.visitLabel(nullPrimitive);
        ArgsClassGenerator.throwNPE(set, "Argument with primitive type cannot be set to NULL");
        set.visitInsn(177);
        set.visitMaxs(maxStack, 2);
        set.visitEnd();
    }

    private static void throwNPE(MethodVisitorEx method, String message) {
        method.visitTypeInsn(187, "java/lang/NullPointerException");
        method.visitInsn(89);
        method.visitLdcInsn(message);
        method.visitMethodInsn(183, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
        method.visitInsn(191);
    }

    private static void throwAIOOBE(MethodVisitorEx method, int arg) {
        method.visitTypeInsn(187, "org/spongepowered/asm/mixin/injection/invoke/arg/ArgumentIndexOutOfBoundsException");
        method.visitInsn(89);
        method.visitVarInsn(21, arg);
        method.visitMethodInsn(183, "org/spongepowered/asm/mixin/injection/invoke/arg/ArgumentIndexOutOfBoundsException", "<init>", "(I)V", false);
        method.visitInsn(191);
    }

    private static void box(MethodVisitor method, Type var) {
        String boxingType = Bytecode.getBoxingType(var);
        if (boxingType != null) {
            String desc = String.format("(%s)L%s;", var.getDescriptor(), boxingType);
            method.visitMethodInsn(184, boxingType, "valueOf", desc, false);
        }
    }

    private static void unbox(MethodVisitor method, Type var) {
        String boxingType = Bytecode.getBoxingType(var);
        if (boxingType != null) {
            String unboxingMethod = Bytecode.getUnboxingMethod(var);
            String desc = "()" + var.getDescriptor();
            method.visitTypeInsn(192, boxingType);
            method.visitMethodInsn(182, boxingType, unboxingMethod, desc, false);
        } else {
            method.visitTypeInsn(192, var.getInternalName());
        }
    }

    class ArgsClassInfo
    extends SyntheticClassInfo {
        final String desc;
        final Type[] args;
        int loaded;

        ArgsClassInfo(IMixinInfo mixin, String name, String desc) {
            super(mixin, name);
            this.loaded = 0;
            this.desc = desc;
            this.args = Type.getArgumentTypes((String)desc);
        }

        @Override
        public boolean isLoaded() {
            return this.loaded > 0;
        }

        String getSignature() {
            return new SignaturePrinter("", null, this.args).setFullyQualified(true).getFormattedArgs();
        }
    }
}

