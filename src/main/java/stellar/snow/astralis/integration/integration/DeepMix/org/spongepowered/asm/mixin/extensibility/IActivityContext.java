package org.spongepowered.asm.mixin.extensibility;

public interface IActivityContext {
    public String toString(String var1);

    public IActivity begin(String var1, Object ... var2);

    public IActivity begin(String var1);

    public void clear();

    public static interface IActivity {
        public void next(String var1, Object ... var2);

        public void next(String var1);

        public void end();

        public void append(String var1, Object ... var2);

        public void append(String var1);
    }
}

