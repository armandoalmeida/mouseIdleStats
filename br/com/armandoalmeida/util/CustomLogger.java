package br.com.armandoalmeida.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * CustomLogger is an extension of the {@link Logger} class designed to handle logging
 * during the shutdown process when the default logger may not be available.
 * This logger ensures that its handlers are not removed until the shutdown method is called,
 * allowing for proper logging even during application termination.
 *
 * <p>This class maintains a list of all instances to manage their lifecycle effectively.
 * It provides a static method to obtain a logger instance with a specified logging level
 * and name, and it includes a shutdown method to flush and reset all logger handlers.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * CustomLogger logger = CustomLogger.getLogger(Level.INFO, "MyLogger");
 * logger.info("This is an info message.");
 * logger.shutdown();
 * </pre>
 */
public class CustomLogger extends Logger {
    private static final List<CustomLogger> instances = new ArrayList<>();
    private boolean canRemoveHandler = false;

    protected CustomLogger(Level level, String name) {
        super(name, null);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new LoggerFormatter());
        consoleHandler.setLevel(level);
        this.setUseParentHandlers(false);
        this.addHandler(consoleHandler);
        this.setLevel(level);

        LogManager.getLogManager().addLogger(this);
    }

    /**
     * Retrieves a new instance of CustomLogger with the specified logging level and name.
     *
     * @param level the logging level for this logger
     * @param name  the name of the logger
     * @return a CustomLogger instance
     */
    public static CustomLogger getLogger(Level level, String name) {
        CustomLogger customLogger = new CustomLogger(level, name);
        instances.add(customLogger);
        return customLogger;
    }

    /**
     * Shuts down all instances of CustomLogger, flushing their handlers and resetting the LogManager.
     * This method should be called during application shutdown to ensure all log messages are processed.
     */
    public void shutdown() {
        instances.forEach(customLogger -> {
            customLogger.canRemoveHandler = true;
            Arrays.stream(customLogger.getHandlers()).forEach(Handler::flush);
        });
        LogManager.getLogManager().reset();
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        if (canRemoveHandler) {
            super.removeHandler(handler);
        }
    }
}
