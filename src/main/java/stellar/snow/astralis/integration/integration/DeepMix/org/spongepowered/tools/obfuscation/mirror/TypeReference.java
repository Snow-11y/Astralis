package org.spongepowered.tools.obfuscation.mirror;

import java.io.Serializable;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

public class TypeReference
implements Serializable,
Comparable<TypeReference> {
    private final String name;
    private transient TypeHandle handle;

    public TypeReference(TypeHandle handle) {
        this.name = handle.getName();
        this.handle = handle;
    }

    public TypeReference(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getClassName() {
        return this.name.replace('/', '.');
    }

    public TypeHandle getHandle(ITypeHandleProvider typeHandleProvider) {
        if (this.handle == null) {
            try {
                this.handle = typeHandleProvider.getTypeHandle(this.name);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return this.handle;
    }

    public String toString() {
        return String.format("TypeReference[%s]", this.name);
    }

    @Override
    public int compareTo(TypeReference other) {
        return other == null ? -1 : this.name.compareTo(other.name);
    }

    public boolean equals(Object other) {
        return other instanceof TypeReference && this.compareTo((TypeReference)other) == 0;
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}

