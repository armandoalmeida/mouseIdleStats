package br.com.armandoalmeida.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggerFormatter extends Formatter {

    @Override
    public String format(LogRecord logRecord) {
        String callerClassName = null;
        String callerMethodName = null;

        try {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                if (className.contains(logRecord.getLoggerName()) && !className.equals(LoggerFormatter.class.getName())) {
                    callerClassName = className;
                    callerMethodName = methodName;
                    break;
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %2$s [%3$s] (%5$s.%6$s) %4$s%n",
                logRecord.getMillis(), logRecord.getLevel().getName(),
                Thread.currentThread().getName(), logRecord.getMessage(),
                callerClassName, callerMethodName);
    }
}
