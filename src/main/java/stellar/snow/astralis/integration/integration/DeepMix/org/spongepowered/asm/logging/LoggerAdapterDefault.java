package org.spongepowered.asm.logging;

import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

public class LoggerAdapterDefault
extends LoggerAdapterAbstract {
    public LoggerAdapterDefault(String name) {
        super(name);
    }

    @Override
    public String getType() {
        return "Default Logger (No Logging)";
    }

    @Override
    public void catching(Level level, Throwable t) {
    }

    @Override
    public void log(Level level, String message, Object ... params) {
    }

    @Override
    public void log(Level level, String message, Throwable t) {
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        return null;
    }
}

