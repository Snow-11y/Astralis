package org.spongepowered.asm.logging;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

public class LoggerAdapterJava
extends LoggerAdapterAbstract {
    private static final java.util.logging.Level[] LEVELS = new java.util.logging.Level[]{java.util.logging.Level.SEVERE, java.util.logging.Level.SEVERE, java.util.logging.Level.WARNING, java.util.logging.Level.INFO, java.util.logging.Level.FINE, java.util.logging.Level.FINER};
    private final Logger logger;

    public LoggerAdapterJava(String name) {
        super(name);
        this.logger = LoggerAdapterJava.getLogger(name);
    }

    @Override
    public String getType() {
        return "java.util.logging Log Adapter";
    }

    @Override
    public void catching(Level level, Throwable t) {
        this.warn("Catching {}: {}", t.getClass().getName(), t.getMessage(), t);
    }

    @Override
    public void debug(String message, Object ... params) {
        LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
        this.logger.fine(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.fine(formatted.getThrowable().toString());
        }
    }

    @Override
    public void debug(String message, Throwable t) {
        this.logger.fine(message);
        this.logger.fine(t.toString());
    }

    @Override
    public void error(String message, Object ... params) {
        LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
        this.logger.severe(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.severe(formatted.getThrowable().toString());
        }
    }

    @Override
    public void error(String message, Throwable t) {
        this.logger.severe(message);
        this.logger.severe(t.toString());
    }

    @Override
    public void fatal(String message, Object ... params) {
        LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
        this.logger.severe(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.severe(formatted.getThrowable().toString());
        }
    }

    @Override
    public void fatal(String message, Throwable t) {
        this.logger.severe(message);
        this.logger.severe(t.toString());
    }

    @Override
    public void info(String message, Object ... params) {
        LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
        this.logger.info(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.info(formatted.getThrowable().toString());
        }
    }

    @Override
    public void info(String message, Throwable t) {
        this.logger.info(message);
        this.logger.info(t.toString());
    }

    @Override
    public void log(Level level, String message, Object ... params) {
        java.util.logging.Level logLevel = LEVELS[level.ordinal()];
        LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
        this.logger.log(logLevel, formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.log(LEVELS[level.ordinal()], formatted.getThrowable().toString());
        }
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        java.util.logging.Level logLevel = LEVELS[level.ordinal()];
        this.logger.log(logLevel, message);
        this.logger.log(logLevel, t.toString());
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        this.warn("Throwing {}: {}", t.getClass().getName(), t.getMessage(), t);
        return t;
    }

    @Override
    public void trace(String message, Object ... params) {
        LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
        this.logger.finer(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.finer(formatted.getThrowable().toString());
        }
    }

    @Override
    public void trace(String message, Throwable t) {
        this.logger.finer(message);
        this.logger.finer(t.toString());
    }

    @Override
    public void warn(String message, Object ... params) {
        LoggerAdapterAbstract.FormattedMessage formatted = new LoggerAdapterAbstract.FormattedMessage(message, params);
        this.logger.warning(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.warning(formatted.getThrowable().toString());
        }
    }

    @Override
    public void warn(String message, Throwable t) {
        this.logger.warning(message);
        this.logger.warning(t.toString());
    }

    private static Logger getLogger(String name) {
        LogManager logManager = LogManager.getLogManager();
        Logger logger = logManager.getLogger(name);
        if (logger != null) {
            return logger;
        }
        return LogManager.getLogManager().getLogger("global");
    }
}

