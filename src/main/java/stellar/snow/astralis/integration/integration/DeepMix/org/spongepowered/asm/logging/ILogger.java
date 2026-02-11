package org.spongepowered.asm.logging;

import org.spongepowered.asm.logging.Level;

public interface ILogger {
    public String getId();

    public String getType();

    public void catching(Level var1, Throwable var2);

    public void catching(Throwable var1);

    public void debug(String var1, Object ... var2);

    public void debug(String var1, Throwable var2);

    public void error(String var1, Object ... var2);

    public void error(String var1, Throwable var2);

    public void fatal(String var1, Object ... var2);

    public void fatal(String var1, Throwable var2);

    public void info(String var1, Object ... var2);

    public void info(String var1, Throwable var2);

    public void log(Level var1, String var2, Object ... var3);

    public void log(Level var1, String var2, Throwable var3);

    public <T extends Throwable> T throwing(T var1);

    public void trace(String var1, Object ... var2);

    public void trace(String var1, Throwable var2);

    public void warn(String var1, Object ... var2);

    public void warn(String var1, Throwable var2);
}

