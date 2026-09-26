package com.rtm516.nethernettester.bootstrap;

import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class Logger extends SimpleTerminalConsole implements com.rtm516.nethernettester.Logger {
    private final org.slf4j.Logger logger;
    private final String prefixString;

    public Logger(org.slf4j.Logger logger) {
        this(logger, "");
    }

    public Logger(org.slf4j.Logger logger, String prefixString) {
        this.logger = logger;
        this.prefixString = prefixString;
    }

    public void info(String message) {
        logger.info(prefix(message));
    }

    public void warn(String message) {
        logger.warn(prefix(message));
    }

    public void error(String message) {
        logger.error(prefix(message));
    }

    public void error(String message, Throwable ex) {
        logger.error(prefix(message), ex);
    }

    public void debug(String message) {
        logger.debug(prefix(message));
    }

    public Logger prefixed(String prefixString) {
        return new Logger(logger, prefixString);
    }

    private String prefix(String message) {
        if (prefixString.isEmpty()) {
            return message;
        } else {
            return "[" + prefixString + "] " + message;
        }
    }

    public void setDebug(boolean debug) {
        Configurator.setLevel(logger.getName(), debug ? Level.DEBUG : Level.INFO);
    }

    @Override
    protected boolean isRunning() {
        return true;
    }

    @Override
    protected void runCommand(String command) {

    }

    @Override
    protected void shutdown() {

    }
}
