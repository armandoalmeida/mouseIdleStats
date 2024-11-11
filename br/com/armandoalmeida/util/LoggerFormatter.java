package br.com.armandoalmeida.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * LoggerFormatter is a custom formatter for log records that provides a structured
 * and informative log message format. It includes the timestamp, log level,
 * thread name, caller class and method names, and the log message itself.
 *
 * <p>This formatter enhances log readability and helps in debugging by clearly
 * indicating where the log entry originated.</p>
 *
 * <p>Example log output:</p>
 * <pre>
 * 2024-11-10 14:30:15 INFO [main] (com.example.MyClass.myMethod) This is a log message
 * </pre>
 */
public class LoggerFormatter extends Formatter {

    @Override
    public String format(LogRecord logRecord) {
        String callerClassName = null;
        String callerMethodName = null;

        // Retrieve the stack trace elements to find the caller's class and method
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains(logRecord.getLoggerName()) && !className.equals(LoggerFormatter.class.getName())) {
                callerClassName = className;
                callerMethodName = element.getMethodName();
                break;
            }
        }

        // Format the log message with the desired structure
        return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %2$s [%3$s] (%4$s.%5$s) %6$s%n",
                logRecord.getMillis(), logRecord.getLevel().getName(),
                Thread.currentThread().getName(), callerClassName, callerMethodName,
                logRecord.getMessage());
    }
}
