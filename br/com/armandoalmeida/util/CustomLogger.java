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
 * This class was created because the default {@link Logger} can be not available during the shutdown process. It won't
 * remove the logger handlers until the method shutdown be called.
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

    public static CustomLogger getLogger(Level level, String name) {
        CustomLogger customLogger = new CustomLogger(level, name);
        instances.add(customLogger);
        return customLogger;
    }

    public void shutdown() {
        instances.forEach(customLogger -> {
            customLogger.canRemoveHandler = true;
            Arrays.stream(customLogger.getHandlers()).forEach(Handler::flush);
        });
        LogManager.getLogManager().reset();
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        if (canRemoveHandler)
            super.removeHandler(handler);
    }
}
