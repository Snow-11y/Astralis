package org.spongepowered.asm.logging;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;
import org.spongepowered.include.com.google.common.base.Strings;

public class LoggerAdapterConsole
extends LoggerAdapterAbstract {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private PrintStream debug;

    public LoggerAdapterConsole(String name) {
        super(Strings.nullToEmpty(name));
    }

    @Override
    public String getType() {
        return "Default Console Logger";
    }

    public LoggerAdapterConsole setDebugStream(PrintStream debug) {
        this.debug = debug;
        return this;
    }

    @Override
    public void catching(Level level, Throwable t) {
        this.log(Level.WARN, "Catching {}: {}", t.getClass().getName(), t.getMessage(), t);
    }

    @Override
    public void log(Level level, String message, Object ... params) {
        PrintStream out = this.getOutputStream(level);
        if (out != null) {
            LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
            out.println(String.format("[%s] [%s/%s] %s", new Object[]{DATE_FORMAT.format(new Date()), this.getId(), level, formatted}));
            if (formatted.hasThrowable()) {
                formatted.getThrowable().printStackTrace(out);
            }
        }
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        PrintStream out = this.getOutputStream(level);
        if (out != null) {
            out.println(String.format("[%s] [%s/%s] %s", new Object[]{DATE_FORMAT.format(new Date()), this.getId(), level, message}));
            t.printStackTrace(out);
        }
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        this.log(Level.WARN, "Throwing {}: {}", t.getClass().getName(), t.getMessage(), t);
        return t;
    }

    private PrintStream getOutputStream(Level level) {
        if (level == Level.FATAL || level == Level.ERROR || level == Level.WARN) {
            return System.err;
        }
        if (level == Level.INFO) {
            return System.out;
        }
        if (level == Level.DEBUG) {
            return this.debug;
        }
        return null;
    }
}

