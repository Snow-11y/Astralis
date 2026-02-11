package org.spongepowered.asm.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.Lists;

public final class Annotations {
    private static final Class<?>[] MERGEABLE_MIXIN_ANNOTATIONS = new Class[]{Overwrite.class, Intrinsic.class, Final.class, Debug.class};
    private static Pattern mergeableAnnotationPattern = Annotations.getMergeableAnnotationPattern();

    private Annotations() {
    }

    public static IAnnotationHandle handleOf(Object annotation) {
        if (annotation instanceof IAnnotationHandle) {
            return (IAnnotationHandle)annotation;
        }
        if (annotation instanceof AnnotationNode) {
            return new Handle((AnnotationNode)annotation);
        }
        if (annotation == null) {
            return null;
        }
        throw new IllegalArgumentException("Unsupported annotation type: " + annotation.getClass().getName());
    }

    public static String getDesc(Class<? extends Annotation> annotationType) {
        return Type.getType(annotationType).getInternalName();
    }

    public static String getSimpleName(Class<? extends Annotation> annotationType) {
        return annotationType.getSimpleName();
    }

    public static String getSimpleName(AnnotationNode annotation) {
        return Bytecode.getSimpleName(annotation.desc);
    }

    public static void setVisible(FieldNode field, Class<? extends Annotation> annotationClass, Object ... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        field.visibleAnnotations = Annotations.add(field.visibleAnnotations, node);
    }

    public static void setInvisible(FieldNode field, Class<? extends Annotation> annotationClass, Object ... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        field.invisibleAnnotations = Annotations.add(field.invisibleAnnotations, node);
    }

    public static void setVisible(MethodNode method, Class<? extends Annotation> annotationClass, Object ... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        method.visibleAnnotations = Annotations.add(method.visibleAnnotations, node);
    }

    public static void setInvisible(MethodNode method, Class<? extends Annotation> annotationClass, Object ... value) {
        AnnotationNode node = Annotations.createNode(Type.getDescriptor(annotationClass), value);
        method.invisibleAnnotations = Annotations.add(method.invisibleAnnotations, node);
    }

    private static AnnotationNode createNode(String annotationType, Object ... value) {
        AnnotationNode node = new AnnotationNode(annotationType);
        for (int pos = 0; pos < value.length - 1; pos += 2) {
            if (!(value[pos] instanceof String)) {
                throw new IllegalArgumentException("Annotation keys must be strings, found " + value[pos].getClass().getSimpleName() + " with " + value[pos].toString() + " at index " + pos + " creating " + annotationType);
            }
            node.visit((String)value[pos], value[pos + 1]);
        }
        return node;
    }

    private static List<AnnotationNode> add(List<AnnotationNode> annotations, AnnotationNode node) {
        if (annotations == null) {
            annotations = new ArrayList<AnnotationNode>(1);
        } else {
            annotations.remove(Annotations.get(annotations, node.desc));
        }
        annotations.add(node);
        return annotations;
    }

    public static AnnotationNode getVisible(FieldNode field, Class<? extends Annotation> annotationClass) {
        return Annotations.get(field.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    public static AnnotationNode getInvisible(FieldNode field, Class<? extends Annotation> annotationClass) {
        return Annotations.get(field.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    public static AnnotationNode getVisible(MethodNode method, Class<? extends Annotation> annotationClass) {
        return Annotations.get(method.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    public static AnnotationNode getInvisible(MethodNode method, Class<? extends Annotation> annotationClass) {
        return Annotations.get(method.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    public static AnnotationNode getSingleVisible(MethodNode method, Class<? extends Annotation> ... annotationClasses) {
        return Annotations.getSingle(method.visibleAnnotations, annotationClasses);
    }

    public static AnnotationNode getSingleInvisible(MethodNode method, Class<? extends Annotation> ... annotationClasses) {
        return Annotations.getSingle(method.invisibleAnnotations, annotationClasses);
    }

    public static AnnotationNode getVisible(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return Annotations.get(classNode.visibleAnnotations, Type.getDescriptor(annotationClass));
    }

    public static AnnotationNode getInvisible(ClassNode classNode, Class<? extends Annotation> annotationClass) {
        return Annotations.get(classNode.invisibleAnnotations, Type.getDescriptor(annotationClass));
    }

    public static AnnotationNode getVisibleParameter(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
        if (paramIndex < 0) {
            return Annotations.getVisible(method, annotationClass);
        }
        return Annotations.getParameter(method.visibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
    }

    public static AnnotationNode getInvisibleParameter(MethodNode method, Class<? extends Annotation> annotationClass, int paramIndex) {
        if (paramIndex < 0) {
            return Annotations.getInvisible(method, annotationClass);
        }
        return Annotations.getParameter(method.invisibleParameterAnnotations, Type.getDescriptor(annotationClass), paramIndex);
    }

    public static AnnotationNode getParameter(List<AnnotationNode>[] parameterAnnotations, String annotationType, int paramIndex) {
        if (parameterAnnotations == null || paramIndex < 0 || paramIndex >= parameterAnnotations.length) {
            return null;
        }
        return Annotations.get(parameterAnnotations[paramIndex], annotationType);
    }

    public static AnnotationNode get(List<AnnotationNode> annotations, String annotationType) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            if (!annotationType.equals(annotation.desc)) continue;
            return annotation;
        }
        return null;
    }

    private static AnnotationNode getSingle(List<AnnotationNode> annotations, Class<? extends Annotation>[] annotationClasses) {
        ArrayList<AnnotationNode> nodes = new ArrayList<AnnotationNode>();
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            AnnotationNode annotation = Annotations.get(annotations, Type.getDescriptor(annotationClass));
            if (annotation == null) continue;
            nodes.add(annotation);
        }
        int foundNodes = nodes.size();
        if (foundNodes > 1) {
            throw new IllegalArgumentException("Conflicting annotations found: " + Lists.transform(nodes, new Function<AnnotationNode, String>(){

                @Override
                public String apply(AnnotationNode input) {
                    return input.desc;
                }
            }));
        }
        return foundNodes == 0 ? null : (AnnotationNode)nodes.get(0);
    }

    public static <T> T getValue(AnnotationNode annotation) {
        return Annotations.getValue(annotation, "value");
    }

    public static <T> T getValue(AnnotationNode annotation, String key, T defaultValue) {
        T returnValue = Annotations.getValue(annotation, key);
        return returnValue != null ? returnValue : defaultValue;
    }

    public static <T> T getValue(AnnotationNode annotation, String key, Class<?> annotationClass) {
        Preconditions.checkNotNull(annotationClass, "annotationClass cannot be null");
        Object value = Annotations.getValue(annotation, key);
        if (value == null) {
            try {
                value = annotationClass.getDeclaredMethod(key, new Class[0]).getDefaultValue();
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
        }
        return value;
    }

    public static <T> T getValue(AnnotationNode annotation, String key) {
        boolean getNextValue = false;
        if (annotation == null || annotation.values == null) {
            return null;
        }
        for (Object value : annotation.values) {
            if (getNextValue) {
                return (T)value;
            }
            if (!value.equals(key)) continue;
            getNextValue = true;
        }
        return null;
    }

    public static <T extends Enum<T>> T getValue(AnnotationNode annotation, String key, Class<T> enumClass, T defaultValue) {
        String[] value = (String[])Annotations.getValue(annotation, key);
        if (value == null) {
            return defaultValue;
        }
        return Annotations.toEnumValue(enumClass, value);
    }

    public static <T> List<T> getValue(AnnotationNode annotation, String key, boolean notNull) {
        T value = Annotations.getValue(annotation, key);
        if (value instanceof List) {
            return (List)value;
        }
        if (value != null) {
            ArrayList<T> list = new ArrayList<T>();
            list.add(value);
            return list;
        }
        return Collections.emptyList();
    }

    public static <T extends Enum<T>> List<T> getValue(AnnotationNode annotation, String key, boolean notNull, Class<T> enumClass) {
        T value = Annotations.getValue(annotation, key);
        if (value instanceof List) {
            ListIterator<T> iter = ((List)value).listIterator();
            while (iter.hasNext()) {
                iter.set(Annotations.toEnumValue(enumClass, (String[])iter.next()));
            }
            return (List)value;
        }
        if (value instanceof String[]) {
            ArrayList<T> list = new ArrayList<T>();
            list.add(Annotations.toEnumValue(enumClass, (String[])value));
            return list;
        }
        return Collections.emptyList();
    }

    public static void setValue(AnnotationNode annotation, String key, Object value) {
        if (annotation == null) {
            return;
        }
        int existingIndex = 0;
        if (annotation.values != null) {
            for (int pos = 0; pos < annotation.values.size() - 1; pos += 2) {
                String keyName = annotation.values.get(pos).toString();
                if (!key.equals(keyName)) continue;
                existingIndex = pos + 1;
                break;
            }
        } else {
            annotation.values = new ArrayList();
        }
        if (existingIndex > 0) {
            annotation.values.set(existingIndex, Annotations.packValue(value));
            return;
        }
        annotation.values.add(key);
        annotation.values.add(Annotations.packValue(value));
    }

    private static Object packValue(Object value) {
        Class<?> type = value.getClass();
        if (type.isEnum()) {
            return new String[]{Type.getDescriptor(type), value.toString()};
        }
        return value;
    }

    public static void merge(ClassNode from, ClassNode to) {
        to.visibleAnnotations = Annotations.merge(from.visibleAnnotations, to.visibleAnnotations, "class", from.name);
        to.invisibleAnnotations = Annotations.merge(from.invisibleAnnotations, to.invisibleAnnotations, "class", from.name);
    }

    public static void merge(MethodNode from, MethodNode to) {
        to.visibleAnnotations = Annotations.merge(from.visibleAnnotations, to.visibleAnnotations, "method", from.name);
        to.invisibleAnnotations = Annotations.merge(from.invisibleAnnotations, to.invisibleAnnotations, "method", from.name);
    }

    public static void merge(FieldNode from, FieldNode to) {
        to.visibleAnnotations = Annotations.merge(from.visibleAnnotations, to.visibleAnnotations, "field", from.name);
        to.invisibleAnnotations = Annotations.merge(from.invisibleAnnotations, to.invisibleAnnotations, "field", from.name);
    }

    private static List<AnnotationNode> merge(List<AnnotationNode> from, List<AnnotationNode> to, String type, String name) {
        try {
            if (from == null) {
                return to;
            }
            if (to == null) {
                to = new ArrayList<AnnotationNode>();
            }
            for (AnnotationNode annotation : from) {
                if (!Annotations.isMergeableAnnotation(annotation)) continue;
                Iterator<AnnotationNode> iter = to.iterator();
                while (iter.hasNext()) {
                    if (!iter.next().desc.equals(annotation.desc)) continue;
                    iter.remove();
                    break;
                }
                to.add(annotation);
            }
        }
        catch (Exception ex) {
            MixinService.getService().getLogger("mixin").warn("Exception encountered whilst merging annotations for {} {}", type, name);
        }
        return to;
    }

    private static boolean isMergeableAnnotation(AnnotationNode annotation) {
        if (annotation.desc.startsWith("L" + Constants.MIXIN_PACKAGE_REF)) {
            return mergeableAnnotationPattern.matcher(annotation.desc).matches();
        }
        return true;
    }

    private static Pattern getMergeableAnnotationPattern() {
        StringBuilder sb = new StringBuilder("^L(");
        for (int i = 0; i < MERGEABLE_MIXIN_ANNOTATIONS.length; ++i) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(MERGEABLE_MIXIN_ANNOTATIONS[i].getName().replace('.', '/'));
        }
        return Pattern.compile(sb.append(");$").toString());
    }

    private static <T extends Enum<T>> T toEnumValue(Class<T> enumClass, String[] value) {
        if (!enumClass.getName().equals(Type.getType((String)value[0]).getClassName())) {
            throw new IllegalArgumentException("The supplied enum class does not match the stored enum value");
        }
        return Enum.valueOf(enumClass, value[1]);
    }

    public static class Handle
    implements IAnnotationHandle {
        private final AnnotationNode annotation;

        Handle(AnnotationNode annotation) {
            Preconditions.checkNotNull(annotation, "annotation");
            this.annotation = annotation;
        }

        @Override
        public boolean exists() {
            return true;
        }

        public AnnotationNode getNode() {
            return this.annotation;
        }

        @Override
        public String getDesc() {
            return Type.getType((String)this.annotation.desc).getInternalName();
        }

        @Override
        public List<IAnnotationHandle> getAnnotationList(String key) {
            List<AnnotationNode> value = Annotations.getValue(this.annotation, key, false);
            ArrayList<IAnnotationHandle> list = new ArrayList<IAnnotationHandle>();
            if (value != null) {
                for (AnnotationNode node : value) {
                    list.add(new Handle(node));
                }
            }
            return list;
        }

        @Override
        public Type getTypeValue(String key) {
            return this.getValue(key, Type.VOID_TYPE);
        }

        @Override
        public List<Type> getTypeList(String key) {
            return this.getList(key);
        }

        @Override
        public IAnnotationHandle getAnnotation(String key) {
            AnnotationNode value = (AnnotationNode)Annotations.getValue(this.annotation, key);
            return value != null ? new Handle(value) : null;
        }

        @Override
        public <T> T getValue(String key, T defaultValue) {
            return Annotations.getValue(this.annotation, key, defaultValue);
        }

        @Override
        public <T> T getValue() {
            return this.getValue("value", null);
        }

        @Override
        public <T> T getValue(String key) {
            return this.getValue(key, null);
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return this.getValue(key, defaultValue);
        }

        @Override
        public <T> List<T> getList() {
            return this.getList("value");
        }

        @Override
        public <T> List<T> getList(String key) {
            return Annotations.getValue(this.annotation, key, false);
        }

        public String toString() {
            return "@" + Annotations.getSimpleName(this.annotation);
        }
    }
}

