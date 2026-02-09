package org.spongepowered.asm.logging;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;

public abstract class LoggerAdapterAbstract
implements ILogger {
    private final String id;

    protected LoggerAdapterAbstract(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void catching(Throwable t) {
        this.catching(Level.WARN, t);
    }

    @Override
    public void debug(String message, Object ... params) {
        this.log(Level.DEBUG, message, params);
    }

    @Override
    public void debug(String message, Throwable t) {
        this.log(Level.DEBUG, message, t);
    }

    @Override
    public void error(String message, Object ... params) {
        this.log(Level.ERROR, message, params);
    }

    @Override
    public void error(String message, Throwable t) {
        this.log(Level.ERROR, message, t);
    }

    @Override
    public void fatal(String message, Object ... params) {
        this.log(Level.FATAL, message, params);
    }

    @Override
    public void fatal(String message, Throwable t) {
        this.log(Level.FATAL, message, t);
    }

    @Override
    public void info(String message, Object ... params) {
        this.log(Level.INFO, message, params);
    }

    @Override
    public void info(String message, Throwable t) {
        this.log(Level.INFO, message, t);
    }

    @Override
    public void trace(String message, Object ... params) {
        this.log(Level.TRACE, message, params);
    }

    @Override
    public void trace(String message, Throwable t) {
        this.log(Level.TRACE, message, t);
    }

    @Override
    public void warn(String message, Object ... params) {
        this.log(Level.WARN, message, params);
    }

    @Override
    public void warn(String message, Throwable t) {
        this.log(Level.WARN, message, t);
    }

    public static class FormattedMessage {
        private String message;
        private Throwable t;

        public FormattedMessage(String message, Object ... params) {
            int delimPos;
            int param;
            if (params.length == 0) {
                this.message = message;
                return;
            }
            StringBuilder sb = new StringBuilder();
            int pos = 0;
            for (param = 0; pos < message.length() && param < params.length && (delimPos = message.indexOf("{}", pos)) >= 0; ++param) {
                sb.append(message.substring(pos, delimPos)).append(params[param]);
                pos = delimPos + 2;
            }
            if (pos < message.length()) {
                sb.append(message.substring(pos));
            }
            if (param < params.length && params[params.length - 1] instanceof Throwable) {
                this.t = (Throwable)params[params.length - 1];
            }
            this.message = sb.toString();
        }

        public String toString() {
            return this.message;
        }

        public String getMessage() {
            return this.message;
        }

        public boolean hasThrowable() {
            return this.t != null;
        }

        public Throwable getThrowable() {
            return this.t;
        }
    }
}

