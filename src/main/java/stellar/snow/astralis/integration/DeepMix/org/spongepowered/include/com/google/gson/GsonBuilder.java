package org.spongepowered.include.com.google.gson;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.include.com.google.gson.DefaultDateTypeAdapter;
import org.spongepowered.include.com.google.gson.FieldNamingPolicy;
import org.spongepowered.include.com.google.gson.FieldNamingStrategy;
import org.spongepowered.include.com.google.gson.Gson;
import org.spongepowered.include.com.google.gson.InstanceCreator;
import org.spongepowered.include.com.google.gson.LongSerializationPolicy;
import org.spongepowered.include.com.google.gson.TreeTypeAdapter;
import org.spongepowered.include.com.google.gson.TypeAdapterFactory;
import org.spongepowered.include.com.google.gson.internal.Excluder;
import org.spongepowered.include.com.google.gson.reflect.TypeToken;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class GsonBuilder {
    private Excluder excluder = Excluder.DEFAULT;
    private LongSerializationPolicy longSerializationPolicy = LongSerializationPolicy.DEFAULT;
    private FieldNamingStrategy fieldNamingPolicy = FieldNamingPolicy.IDENTITY;
    private final Map<Type, InstanceCreator<?>> instanceCreators = new HashMap();
    private final List<TypeAdapterFactory> factories = new ArrayList<TypeAdapterFactory>();
    private final List<TypeAdapterFactory> hierarchyFactories = new ArrayList<TypeAdapterFactory>();
    private boolean serializeNulls;
    private String datePattern;
    private int dateStyle = 2;
    private int timeStyle = 2;
    private boolean complexMapKeySerialization;
    private boolean serializeSpecialFloatingPointValues;
    private boolean escapeHtmlChars = true;
    private boolean prettyPrinting;
    private boolean generateNonExecutableJson;

    public GsonBuilder setPrettyPrinting() {
        this.prettyPrinting = true;
        return this;
    }

    public GsonBuilder disableHtmlEscaping() {
        this.escapeHtmlChars = false;
        return this;
    }

    public Gson create() {
        ArrayList<TypeAdapterFactory> factories = new ArrayList<TypeAdapterFactory>();
        factories.addAll(this.factories);
        Collections.reverse(factories);
        factories.addAll(this.hierarchyFactories);
        this.addTypeAdaptersForDate(this.datePattern, this.dateStyle, this.timeStyle, factories);
        return new Gson(this.excluder, this.fieldNamingPolicy, this.instanceCreators, this.serializeNulls, this.complexMapKeySerialization, this.generateNonExecutableJson, this.escapeHtmlChars, this.prettyPrinting, this.serializeSpecialFloatingPointValues, this.longSerializationPolicy, factories);
    }

    private void addTypeAdaptersForDate(String datePattern, int dateStyle, int timeStyle, List<TypeAdapterFactory> factories) {
        DefaultDateTypeAdapter dateTypeAdapter;
        if (datePattern != null && !"".equals(datePattern.trim())) {
            dateTypeAdapter = new DefaultDateTypeAdapter(datePattern);
        } else if (dateStyle != 2 && timeStyle != 2) {
            dateTypeAdapter = new DefaultDateTypeAdapter(dateStyle, timeStyle);
        } else {
            return;
        }
        factories.add(TreeTypeAdapter.newFactory(TypeToken.get(Date.class), dateTypeAdapter));
        factories.add(TreeTypeAdapter.newFactory(TypeToken.get(Timestamp.class), dateTypeAdapter));
        factories.add(TreeTypeAdapter.newFactory(TypeToken.get(java.sql.Date.class), dateTypeAdapter));
    }
}

