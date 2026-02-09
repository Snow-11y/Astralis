package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.ArrayLengthDef;
import com.llamalad7.mixinextras.expression.impl.pool.ExactTypeDef;
import com.llamalad7.mixinextras.expression.impl.pool.MemberDefinition;
import com.llamalad7.mixinextras.expression.impl.pool.TypeDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Type;

public class IdentifierPool {
    private final Map<String, List<MemberDefinition>> members = new HashMap<String, List<MemberDefinition>>();
    private final Map<String, List<TypeDefinition>> types = new HashMap<String, List<TypeDefinition>>();

    public IdentifierPool() {
        this.addType("byte", new ExactTypeDef(Type.BYTE_TYPE));
        this.addType("char", new ExactTypeDef(Type.CHAR_TYPE));
        this.addType("double", new ExactTypeDef(Type.DOUBLE_TYPE));
        this.addType("float", new ExactTypeDef(Type.FLOAT_TYPE));
        this.addType("int", new ExactTypeDef(Type.INT_TYPE));
        this.addType("long", new ExactTypeDef(Type.LONG_TYPE));
        this.addType("short", new ExactTypeDef(Type.SHORT_TYPE));
        this.addMember("length", new ArrayLengthDef());
    }

    public boolean matchesMember(String id, FlowValue node) {
        List<MemberDefinition> matching = this.members.get(id);
        if (matching == null) {
            throw new IllegalStateException("Use of undeclared identifier '" + id + '\'');
        }
        return matching.stream().anyMatch(it -> it.matches(node));
    }

    public boolean matchesType(String id, Type type) {
        List<TypeDefinition> matching = this.types.get(id);
        if (matching == null) {
            throw new IllegalStateException("Use of undeclared identifier '" + id + '\'');
        }
        return matching.stream().anyMatch(it -> it.matches(type));
    }

    public void addMember(String id, MemberDefinition entry) {
        this.members.computeIfAbsent(id, k -> new ArrayList()).add(entry);
    }

    public void addType(String id, TypeDefinition entry) {
        this.types.computeIfAbsent(id, k -> new ArrayList()).add(entry);
    }

    public boolean memberExists(String id) {
        return this.members.containsKey(id);
    }

    public boolean typeExists(String id) {
        return this.types.containsKey(id);
    }
}

