package org.spongepowered.tools.obfuscation.mirror;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.FieldHandle;
import org.spongepowered.tools.obfuscation.mirror.FieldHandleASM;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandleASM;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

public class TypeHandleASM
extends TypeHandle {
    private static final Map<String, TypeHandleASM> cache = new HashMap<String, TypeHandleASM>();
    private final ClassNode classNode;

    protected TypeHandleASM(PackageElement pkg, String name, ClassNode classNode, ITypeHandleProvider typeProvider) {
        super(pkg, name, typeProvider);
        this.classNode = classNode;
    }

    @Override
    public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
        AnnotationNode visibleAnnotation = Annotations.getVisible(this.classNode, annotationClass);
        if (visibleAnnotation != null) {
            return Annotations.handleOf(visibleAnnotation);
        }
        AnnotationNode invisibleAnnotation = Annotations.getInvisible(this.classNode, annotationClass);
        if (invisibleAnnotation != null) {
            return Annotations.handleOf(invisibleAnnotation);
        }
        return AnnotationHandle.of(null);
    }

    @Override
    public <T extends Element> List<T> getEnclosedElements(ElementKind ... kind) {
        return super.getEnclosedElements(kind);
    }

    @Override
    public boolean hasTypeMirror() {
        return false;
    }

    @Override
    public TypeMirror getTypeMirror() {
        return null;
    }

    @Override
    public TypeHandle getSuperclass() {
        if (this.classNode.superName == null) {
            return null;
        }
        return this.typeProvider.getTypeHandle(this.classNode.superName);
    }

    @Override
    public List<TypeHandle> getInterfaces() {
        ImmutableList.Builder list = ImmutableList.builder();
        for (String ifaceName : this.classNode.interfaces) {
            TypeHandle iface = this.typeProvider.getTypeHandle(ifaceName);
            if (iface == null) continue;
            list.add(iface);
        }
        return list.build();
    }

    @Override
    public List<MethodHandle> getMethods() {
        ImmutableList.Builder methods = ImmutableList.builder();
        for (MethodNode method : this.classNode.methods) {
            if (method.name.startsWith("<") || (method.access & 0x1000) != 0) continue;
            methods.add(new MethodHandleASM((TypeHandle)this, method));
        }
        return methods.build();
    }

    @Override
    public boolean isPublic() {
        return (this.classNode.access & 1) != 0;
    }

    @Override
    public boolean isImaginary() {
        return false;
    }

    @Override
    public boolean isNotInterface() {
        return (this.classNode.access & 0x200) == 0;
    }

    @Override
    public String findDescriptor(ITargetSelectorByName selector) {
        String desc = selector.getDesc();
        if (desc == null) {
            for (MethodNode method : this.classNode.methods) {
                if (!method.name.equals(selector.getName())) continue;
                desc = method.desc;
                break;
            }
        }
        return desc;
    }

    @Override
    public FieldHandle findField(String name, String type, boolean matchCase) {
        for (FieldNode field : this.classNode.fields) {
            if (!TypeHandleASM.compareElement(field.name, TypeUtils.getJavaSignature(field.desc), name, type, matchCase)) continue;
            return new FieldHandleASM(this, field);
        }
        return null;
    }

    @Override
    public MethodHandle findMethod(String name, String signature, boolean matchCase) {
        for (MethodNode method : this.classNode.methods) {
            if (!TypeHandleASM.compareElement(method.name, TypeUtils.getJavaSignature(method.desc), name, signature, matchCase)) continue;
            return new MethodHandleASM((TypeHandle)this, method);
        }
        return null;
    }

    protected static boolean compareElement(String elementName, String elementType, String name, String type, boolean matchCase) {
        try {
            boolean compared = matchCase ? name.equals(elementName) : name.equalsIgnoreCase(elementName);
            return compared && (type.length() == 0 || type.equals(elementType));
        }
        catch (NullPointerException ex) {
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static TypeHandle of(PackageElement pkg, String name, IMixinAnnotationProcessor ap) {
        String fqName = pkg.getQualifiedName() + "." + name;
        if (cache.containsKey(fqName)) {
            return cache.get(fqName);
        }
        InputStream is = null;
        try {
            Filer filer = ap.getProcessingEnvironment().getFiler();
            try {
                is = filer.getResource(StandardLocation.CLASS_PATH, pkg.getQualifiedName(), name + ".class").openInputStream();
            }
            catch (FileNotFoundException ignored) {
                is = filer.getResource(StandardLocation.PLATFORM_CLASS_PATH, pkg.getQualifiedName(), name + ".class").openInputStream();
            }
            ClassNode classNode = new ClassNode();
            new ClassReader(is).accept((ClassVisitor)classNode, 0);
            TypeHandleASM typeHandle = new TypeHandleASM(pkg, fqName, classNode, ap.getTypeProvider());
            cache.put(fqName, typeHandle);
            TypeHandleASM typeHandleASM = typeHandle;
            return typeHandleASM;
        }
        catch (FileNotFoundException fnfe) {
            cache.put(fqName, null);
        }
        catch (Exception ex) {
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }
}

