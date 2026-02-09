package org.spongepowered.include.com.google.gson.internal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.spongepowered.include.com.google.gson.ExclusionStrategy;
import org.spongepowered.include.com.google.gson.FieldAttributes;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.TypeAdapter;
import org.spongepowered.include.com.google.gson.TypeAdapterFactory;
import org.spongepowered.include.com.google.gson.annotations.Expose;
import org.spongepowered.include.com.google.gson.annotations.Since;
import org.spongepowered.include.com.google.gson.annotations.Until;
import org.spongepowered.include.com.google.gson.reflect.TypeToken;
import org.spongepowered.include.com.google.gson.stream.JsonReader;
import org.spongepowered.include.com.google.gson.stream.JsonWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class Excluder
implements Cloneable,
TypeAdapterFactory {
    public static final Excluder DEFAULT = new Excluder();
    private double version = -1.0;
    private int modifiers = 136;
    private boolean serializeInnerClasses = true;
    private boolean requireExpose;
    private List<ExclusionStrategy> serializationStrategies = Collections.emptyList();
    private List<ExclusionStrategy> deserializationStrategies = Collections.emptyList();

    protected Excluder clone() {
        try {
            return (Excluder)super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        Class<T> rawType = type.getRawType();
        final boolean skipSerialize = this.excludeClass(rawType, true);
        final boolean skipDeserialize = this.excludeClass(rawType, false);
        if (!skipSerialize && !skipDeserialize) {
            return null;
        }
        return new TypeAdapter<T>(){
            private TypeAdapter<T> delegate;

            @Override
            public T read(JsonReader in) throws IOException {
                if (skipDeserialize) {
                    in.skipValue();
                    return null;
                }
                return this.delegate().read(in);
            }

            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (skipSerialize) {
                    out.nullValue();
                    return;
                }
                this.delegate().write(out, value);
            }

            private TypeAdapter<T> delegate() {
                TypeAdapter d = this.delegate;
                return d != null ? d : (this.delegate = gson.getDelegateAdapter(Excluder.this, type));
            }
        };
    }

    public boolean excludeField(Field field, boolean serialize) {
        List<ExclusionStrategy> list;
        Expose annotation;
        if ((this.modifiers & field.getModifiers()) != 0) {
            return true;
        }
        if (this.version != -1.0 && !this.isValidVersion(field.getAnnotation(Since.class), field.getAnnotation(Until.class))) {
            return true;
        }
        if (field.isSynthetic()) {
            return true;
        }
        if (this.requireExpose && ((annotation = field.getAnnotation(Expose.class)) == null || (serialize ? !annotation.serialize() : !annotation.deserialize()))) {
            return true;
        }
        if (!this.serializeInnerClasses && this.isInnerClass(field.getType())) {
            return true;
        }
        if (this.isAnonymousOrLocal(field.getType())) {
            return true;
        }
        List<ExclusionStrategy> list2 = list = serialize ? this.serializationStrategies : this.deserializationStrategies;
        if (!list.isEmpty()) {
            FieldAttributes fieldAttributes = new FieldAttributes(field);
            for (ExclusionStrategy exclusionStrategy : list) {
                if (!exclusionStrategy.shouldSkipField(fieldAttributes)) continue;
                return true;
            }
        }
        return false;
    }

    public boolean excludeClass(Class<?> clazz, boolean serialize) {
        if (this.version != -1.0 && !this.isValidVersion(clazz.getAnnotation(Since.class), clazz.getAnnotation(Until.class))) {
            return true;
        }
        if (!this.serializeInnerClasses && this.isInnerClass(clazz)) {
            return true;
        }
        if (this.isAnonymousOrLocal(clazz)) {
            return true;
        }
        List<ExclusionStrategy> list = serialize ? this.serializationStrategies : this.deserializationStrategies;
        for (ExclusionStrategy exclusionStrategy : list) {
            if (!exclusionStrategy.shouldSkipClass(clazz)) continue;
            return true;
        }
        return false;
    }

    private boolean isAnonymousOrLocal(Class<?> clazz) {
        return !Enum.class.isAssignableFrom(clazz) && (clazz.isAnonymousClass() || clazz.isLocalClass());
    }

    private boolean isInnerClass(Class<?> clazz) {
        return clazz.isMemberClass() && !this.isStatic(clazz);
    }

    private boolean isStatic(Class<?> clazz) {
        return (clazz.getModifiers() & 8) != 0;
    }

    private boolean isValidVersion(Since since, Until until) {
        return this.isValidSince(since) && this.isValidUntil(until);
    }

    private boolean isValidSince(Since annotation) {
        double annotationVersion;
        return annotation == null || !((annotationVersion = annotation.value()) > this.version);
    }

    private boolean isValidUntil(Until annotation) {
        double annotationVersion;
        return annotation == null || !((annotationVersion = annotation.value()) <= this.version);
    }
}

