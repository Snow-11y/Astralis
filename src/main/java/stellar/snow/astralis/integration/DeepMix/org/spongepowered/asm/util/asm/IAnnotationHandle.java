package org.spongepowered.asm.util.asm;

import java.util.List;
import org.objectweb.asm.Type;

public interface IAnnotationHandle {
    public boolean exists();

    public String getDesc();

    public List<IAnnotationHandle> getAnnotationList(String var1);

    public Type getTypeValue(String var1);

    public List<Type> getTypeList(String var1);

    public IAnnotationHandle getAnnotation(String var1);

    public <T> T getValue(String var1, T var2);

    public <T> T getValue();

    public <T> T getValue(String var1);

    public boolean getBoolean(String var1, boolean var2);

    public <T> List<T> getList();

    public <T> List<T> getList(String var1);
}

