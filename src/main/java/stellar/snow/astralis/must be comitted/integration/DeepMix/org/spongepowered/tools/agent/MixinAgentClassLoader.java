package org.spongepowered.tools.agent;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.service.ServiceNotAvailableError;

class MixinAgentClassLoader
extends ClassLoader {
    private Map<Class<?>, byte[]> mixins = new HashMap();
    private Map<String, byte[]> targets = new HashMap<String, byte[]>();

    MixinAgentClassLoader() {
    }

    void addMixinClass(String name) {
        MixinAgentClassLoader.log(Level.DEBUG, "Mixin class {} added to class loader", name);
        try {
            byte[] bytes = this.materialise(name);
            Class<?> clazz = this.defineClass(name, bytes, 0, bytes.length);
            clazz.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            this.mixins.put(clazz, bytes);
        }
        catch (Throwable e) {
            MixinAgentClassLoader.log(Level.ERROR, "Catching {}", e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void addTargetClass(String name, ClassNode classNode) {
        Map<String, byte[]> map = this.targets;
        synchronized (map) {
            if (this.targets.containsKey(name)) {
                return;
            }
            try {
                ClassWriter cw = new ClassWriter(0);
                classNode.accept((ClassVisitor)cw);
                this.targets.put(name, cw.toByteArray());
            }
            catch (Exception ex) {
                MixinAgentClassLoader.log(Level.ERROR, "Error storing original class bytecode for {} in mixin hotswap agent. {}: {}", name, ex.getClass().getName(), ex.getMessage());
                MixinAgentClassLoader.log(Level.DEBUG, ex.toString(), new Object[0]);
            }
        }
    }

    byte[] getFakeMixinBytecode(Class<?> clazz) {
        return this.mixins.get(clazz);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    byte[] getOriginalTargetBytecode(String name) {
        Map<String, byte[]> map = this.targets;
        synchronized (map) {
            return this.targets.get(name);
        }
    }

    private byte[] materialise(String name) {
        ClassWriter cw = new ClassWriter(3);
        cw.visit(MixinEnvironment.getCompatibilityLevel().getClassVersion(), 1, name.replace('.', '/'), null, Type.getInternalName(Object.class), null);
        MethodVisitor mv = cw.visitMethod(1, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(25, 0);
        mv.visitMethodInsn(183, Type.getInternalName(Object.class), "<init>", "()V", false);
        mv.visitInsn(177);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    public static void log(Level level, String message, Object ... params) {
        try {
            MixinService.getService().getLogger("mixin.agent").log(level, message, params);
        }
        catch (ServiceNotAvailableError err) {
            System.err.printf("MixinAgent: %s: %s", level.name(), String.format(message, params));
        }
    }
}

