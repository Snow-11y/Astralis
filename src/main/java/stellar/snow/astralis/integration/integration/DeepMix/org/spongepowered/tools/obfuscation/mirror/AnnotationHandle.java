package org.spongepowered.tools.obfuscation.mirror;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.objectweb.asm.Type;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

public final class AnnotationHandle
implements IAnnotationHandle {
    public static final AnnotationHandle MISSING = new AnnotationHandle(null);
    private final AnnotationMirror annotation;

    private AnnotationHandle(AnnotationMirror annotation) {
        this.annotation = annotation;
    }

    public AnnotationMirror asMirror() {
        return this.annotation;
    }

    @Override
    public boolean exists() {
        return this.annotation != null;
    }

    @Override
    public String getDesc() {
        if (this.annotation == null) {
            return "java/lang/Annotation";
        }
        return TypeUtils.getInternalName(this.annotation.getAnnotationType());
    }

    public String toString() {
        if (this.annotation == null) {
            return "@{UnknownAnnotation}";
        }
        return "@" + this.annotation.getAnnotationType().asElement().getSimpleName();
    }

    @Override
    public <T> T getValue(String key, T defaultValue) {
        if (this.annotation == null) {
            return defaultValue;
        }
        AnnotationValue value = this.getAnnotationValue(key);
        if (defaultValue instanceof Enum && value != null) {
            VariableElement varValue = (VariableElement)value.getValue();
            if (varValue == null) {
                return defaultValue;
            }
            return (T)Enum.valueOf(defaultValue.getClass(), varValue.getSimpleName().toString());
        }
        return (T)(value != null ? value.getValue() : defaultValue);
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
    public IAnnotationHandle getAnnotation(String key) {
        Object mirror;
        Object value = this.getValue(key);
        if (value instanceof AnnotationMirror) {
            return AnnotationHandle.of((AnnotationMirror)value);
        }
        if (value instanceof AnnotationValue && (mirror = ((AnnotationValue)value).getValue()) instanceof AnnotationMirror) {
            return AnnotationHandle.of((AnnotationMirror)mirror);
        }
        return null;
    }

    @Override
    public <T> List<T> getList() {
        return this.getList("value");
    }

    @Override
    public <T> List<T> getList(String key) {
        List<AnnotationValue> list = this.getValue(key, Collections.emptyList());
        return AnnotationHandle.unwrapAnnotationValueList(list);
    }

    @Override
    public List<IAnnotationHandle> getAnnotationList(String key) {
        Object val = this.getValue(key, null);
        if (val == null) {
            return Collections.emptyList();
        }
        if (val instanceof AnnotationMirror) {
            return ImmutableList.of(AnnotationHandle.of(val));
        }
        List list = val;
        ArrayList<AnnotationHandle> annotations = new ArrayList<AnnotationHandle>(list.size());
        for (AnnotationValue value : list) {
            annotations.add(new AnnotationHandle((AnnotationMirror)value.getValue()));
        }
        return Collections.unmodifiableList(annotations);
    }

    @Override
    public Type getTypeValue(String key) {
        TypeMirror typeMirror = (TypeMirror)this.getValue(key);
        return typeMirror == null ? Type.VOID_TYPE : Type.getType((String)TypeUtils.getInternalName(typeMirror));
    }

    @Override
    public List<Type> getTypeList(String key) {
        List<Type> list = this.getList(key);
        ListIterator iter = list.listIterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (!(next instanceof TypeMirror)) continue;
            iter.set(Type.getType((String)TypeUtils.getInternalName((TypeMirror)next)));
        }
        return list;
    }

    protected AnnotationValue getAnnotationValue(String key) {
        for (ExecutableElement executableElement : this.annotation.getElementValues().keySet()) {
            if (!executableElement.getSimpleName().contentEquals(key)) continue;
            return this.annotation.getElementValues().get(executableElement);
        }
        return null;
    }

    protected static <T> List<T> unwrapAnnotationValueList(List<AnnotationValue> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        ArrayList<Object> unfolded = new ArrayList<Object>(list.size());
        for (AnnotationValue value : list) {
            unfolded.add(value.getValue());
        }
        return unfolded;
    }

    protected static AnnotationMirror getAnnotation(Element elem, Class<? extends Annotation> annotationClass) {
        if (elem == null) {
            return null;
        }
        List<? extends AnnotationMirror> annotations = elem.getAnnotationMirrors();
        if (annotations == null) {
            return null;
        }
        for (AnnotationMirror annotationMirror : annotations) {
            TypeElement annotationElement;
            Element element = annotationMirror.getAnnotationType().asElement();
            if (!(element instanceof TypeElement) || !(annotationElement = (TypeElement)element).getQualifiedName().contentEquals(annotationClass.getName())) continue;
            return annotationMirror;
        }
        return null;
    }

    public static AnnotationMirror asMirror(IAnnotationHandle handle) {
        return handle instanceof AnnotationHandle ? ((AnnotationHandle)handle).asMirror() : null;
    }

    public static AnnotationHandle of(AnnotationMirror annotation) {
        return new AnnotationHandle(annotation);
    }

    public static AnnotationHandle of(Element elem, Class<? extends Annotation> annotationClass) {
        return new AnnotationHandle(AnnotationHandle.getAnnotation(elem, annotationClass));
    }
}

