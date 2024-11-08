import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MouseIdleStats {
    private static final CustomLogger log = CustomLogger.getLogger(Level.INFO, MouseIdleStats.class.getName());

    public static void main(String[] args) {
        boolean keepOsAlive = args.length > 0 && args[0].equals("--keep-os-alive");
        new MouseManager(keepOsAlive).run();
    }

    public static class Scheduler implements Runnable {

        private final Runnable command;
        private final Duration duration;
        private final SchedulerType schedulerType;
        private ScheduledFuture<?> scheduledFuture;
        private final ScheduledExecutorService scheduledExecutorService;

        protected Scheduler(SchedulerType schedulerType, Runnable command, Duration duration) {
            this.command = command;
            this.duration = duration;
            this.schedulerType = schedulerType;
            this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        public static Scheduler newPeriodic(Runnable command, Duration duration) {
            return new Scheduler(SchedulerType.PERIODIC, command, duration);
        }

        public static void oneShot(Runnable command, Duration duration) {
            new Scheduler(SchedulerType.ONE_SHOT, command, duration).run();
        }

        @Override
        public void run() {
            if (SchedulerType.ONE_SHOT.equals(schedulerType)) {
                this.schedule(() -> {
                    command.run();
                    this.shutdown();
                }, duration);
            }

            if (SchedulerType.PERIODIC.equals(schedulerType))
                this.scheduleAtFixedRate(command, duration);
        }

        public void shutdown() {
            if (this.scheduledFuture != null)
                this.scheduledFuture.cancel(false);
            this.scheduledExecutorService.shutdown();
        }

        private void scheduleAtFixedRate(Runnable command, Duration duration) {
            this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(command, 0, duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        private void schedule(Runnable command, Duration duration) {
            this.scheduledFuture = this.scheduledExecutorService.schedule(command, duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        private enum SchedulerType {
            PERIODIC, ONE_SHOT
        }

    }

    private static class MouseManager implements Runnable {

        private static final Duration CHECKING_INTERVAL = Duration.ofMinutes(2);
        private static final Duration COUNTER_DELAY = Duration.ofSeconds(1);

        private Point lastPoint;
        private boolean counterCheckActive;
        private LocalDateTime lastCheckDateTime;
        private LocalDateTime lastMovementDateTime;
        private Duration idleTotalTime = Duration.ZERO;

        private final boolean keepOsAlive;
        private final Scheduler counterScheduler;
        private final Scheduler checkerScheduler;

        public MouseManager(boolean keepOsAlive) {
            this.keepOsAlive = keepOsAlive;
            this.checkerScheduler = Scheduler.newPeriodic(this::continuousCheck, CHECKING_INTERVAL);
            this.counterScheduler = Scheduler.newPeriodic(this::counterCheck, COUNTER_DELAY);

            log.info("Started (checking for %s min of idle time)".formatted(CHECKING_INTERVAL.toMinutes()));
            if (keepOsAlive)
                log.info("Keep OS alive during mouse idle checking");
        }

        @Override
        public void run() {
            this.checkerScheduler.run();
            this.counterScheduler.run();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    this.stop();
                } finally {
                    log.shutdown();
                }
            }));
        }

        public void stop() {
            counterScheduler.shutdown();
            checkerScheduler.shutdown();
            checkLastMovement();
            log.info("Done");
        }

        private void continuousCheck() {
            try {
                if (!counterCheckActive)
                    checkMousePosition(true);
            } catch (RuntimeException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void counterCheck() {
            checkMousePosition(false);
        }

        private void checkMousePosition(boolean continuousCheck) {
            if (continuousCheck || counterCheckActive) {
                Point currentPoint = MouseInfo.getPointerInfo().getLocation();
                lastCheckDateTime = LocalDateTime.now();
                log.fine("%s - %s".formatted(lastPoint, currentPoint));
                if (currentPoint.equals(lastPoint)) {
                    startIdleTimeCounter(currentPoint);
                } else {
                    stopIdleTimeCounter();
                    checkLastMovement();
                }
                lastPoint = new Point(currentPoint);
            }
        }

        private void startIdleTimeCounter(Point currentPoint) {
            if (!counterCheckActive) {
                if (lastMovementDateTime == null) {
                    log.info("Starting counting idle time...");
                    lastMovementDateTime = LocalDateTime.from(lastCheckDateTime);
                }

                this.counterCheckActive = true;
            } else {
                if (keepOsAlive)
                    PointerMover.move(currentPoint);
            }
        }

        private void stopIdleTimeCounter() {
            counterCheckActive = false;
        }

        private void checkLastMovement() {
            if (lastMovementDateTime != null) {
                Duration timeDiff = Duration.between(lastMovementDateTime, lastCheckDateTime).plus(CHECKING_INTERVAL);
                idleTotalTime = idleTotalTime.plus(timeDiff);
                log.info("End: %s".formatted(formatDuration(timeDiff)));
                log.info("Total mouse idle time: %s".formatted(formatDuration(idleTotalTime)));
                lastMovementDateTime = null;
            }
        }

        private String formatDuration(Duration duration) {
            return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
        }

    }

    private static class PointerMover {
        private static final Robot robot;

        static {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                throw new IllegalStateException(e);
            }
        }

        public PointerMover() {
            throw new UnsupportedOperationException("Static pointer mover");
        }

        private static synchronized void move(Point stoppedPoint) {
            Direction randomDirection = Direction.values()[ThreadLocalRandom.current().nextInt(4)];
            int randomPixels = ThreadLocalRandom.current().nextInt(90) + 10;
            switch (randomDirection) {
                case UP -> robot.mouseMove(stoppedPoint.x, stoppedPoint.y - randomPixels);
                case DOWN -> robot.mouseMove(stoppedPoint.x, stoppedPoint.y + randomPixels);
                case LEFT -> robot.mouseMove(stoppedPoint.x - randomPixels, stoppedPoint.y);
                case RIGHT -> robot.mouseMove(stoppedPoint.x + randomPixels, stoppedPoint.y);
            }
            restorePosition(stoppedPoint);
        }

        private static void restorePosition(Point stoppedPoint) {
            Scheduler.oneShot(() -> robot.mouseMove(stoppedPoint.x, stoppedPoint.y), Duration.ofMillis(100));
        }

        private enum Direction {
            UP, DOWN, LEFT, RIGHT
        }

    }

    /**
     * This class created because default {@link Logger} can be not available during the shutdown process. It won't
     * remove the logger handlers until the method shutdown be called.
     */
    public static class CustomLogger extends Logger {

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

    private static class LoggerFormatter extends Formatter {
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

}
