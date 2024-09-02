import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MouseIdleStats {
    private static final Logger logger = Logger.getLogger(MouseIdleStats.class.getName());

    static {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new LoggerFormatter());
        logger.setUseParentHandlers(false);
        logger.addHandler(consoleHandler);
    }

    public static void main(String[] args) {
        boolean keepOsAlive = args.length > 0 && args[0].equals("--keep-os-alive");
        final MouseManager mouseManager = new MouseManager(keepOsAlive);
        Runtime.getRuntime().addShutdownHook(new Thread(mouseManager::stop));
    }

    private static class Scheduler {
        public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, Duration duration) {
            return Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(command, 0, duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        public static void schedule(Runnable command, Duration duration) {
            Executors.newSingleThreadScheduledExecutor().schedule(command, duration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private static class MouseManager {

        private static final Duration CHECKING_INTERVAL = Duration.ofMinutes(2);
        private static final Duration COUNTER_DELAY = Duration.ofSeconds(1);

        private Point lastCoordinate;
        private boolean counterScheduledActive;
        private LocalDateTime lastCheckDateTime;
        private LocalDateTime lastMovementDateTime;
        private Duration idleTotalTime = Duration.ZERO;

        private final boolean keepOsAlive;
        private final ScheduledFuture<?> counterScheduledFuture;
        private final ScheduledFuture<?> checkerScheduledFuture;

        public MouseManager(boolean keepOsAlive) {
            this.keepOsAlive = keepOsAlive;
            this.checkerScheduledFuture = Scheduler.scheduleAtFixedRate(this::continuousCheck, CHECKING_INTERVAL);
            this.counterScheduledFuture = Scheduler.scheduleAtFixedRate(this::idlePointerCheck, COUNTER_DELAY);

            logger.info("Started (checking for %s min of idle time)".formatted(CHECKING_INTERVAL.toMinutes()));
            if (keepOsAlive)
                logger.info("Keep OS alive during mouse idle checking");
        }

        public void stop() {
            checkLastMovement();
            PointerMover.shutdown();
            counterScheduledFuture.cancel(false);
            checkerScheduledFuture.cancel(false);
            logger.info("Done");
        }


        private void continuousCheck() {
            try {
                if (!counterScheduledActive)
                    checkMousePosition(true);
            } catch (RuntimeException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void idlePointerCheck() {
            checkMousePosition(false);
        }

        private void checkMousePosition(boolean continuousCheck) {
            if (continuousCheck || counterScheduledActive) {
                Point currentPoint = MouseInfo.getPointerInfo().getLocation();
                lastCheckDateTime = LocalDateTime.now();
                if (currentPoint.equals(lastCoordinate)) {
                    startIdleTimeCounter(currentPoint);
                } else {
                    stopIdleTimeCounter();
                    checkLastMovement();
                }
                lastCoordinate = new Point(currentPoint);
            }
        }

        private void startIdleTimeCounter(Point currentPoint) {
            if (!counterScheduledActive) {
                if (lastMovementDateTime == null) {
                    logger.info("Starting counting idle time...");
                    lastMovementDateTime = lastCheckDateTime;
                }

                this.counterScheduledActive = true;

                if (keepOsAlive)
                    PointerMover.start(currentPoint);
            }
        }

        private void stopIdleTimeCounter() {
            if (keepOsAlive)
                PointerMover.stop();

            counterScheduledActive = false;
        }

        private void checkLastMovement() {
            if (lastMovementDateTime != null) {
                Duration timeDiff = Duration.between(lastMovementDateTime, lastCheckDateTime).plus(CHECKING_INTERVAL);
                idleTotalTime = idleTotalTime.plus(timeDiff);
                logger.info("End: %s".formatted(formatDuration(timeDiff)));
                logger.info("Total mouse idle time: %s".formatted(formatDuration(idleTotalTime)));
                lastMovementDateTime = null;
            }
        }

        public String formatDuration(Duration duration) {
            return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
        }

    }

    private static class PointerMover {
        private static final Duration POINTER_MOVE_SCHEDULER_DURATION = MouseManager.COUNTER_DELAY.dividedBy(2);
        private static final Duration POINTER_MOVE_DELAY = Duration.ofMillis(100);

        private static final Random random = new Random();
        private static final ScheduledFuture<?> scheduledPointerMove = Scheduler.scheduleAtFixedRate(PointerMover::move, POINTER_MOVE_SCHEDULER_DURATION);

        private static Point stoppedPoint;
        private static final Robot robot;

        static {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        }

        public static void start(Point currentPoint) {
            stoppedPoint = currentPoint;
        }

        public static void stop() {
            stoppedPoint = null;
        }

        public static void shutdown() {
            scheduledPointerMove.cancel(true);
        }

        private static void move() {
            if (stoppedPoint != null) {
                Direction randomDirection = Direction.values()[random.nextInt(4)];
                int randomPixels = random.nextInt(90) + 10;
                int x = stoppedPoint.x;
                int y = stoppedPoint.y;

                switch (randomDirection) {
                    case UP -> robot.mouseMove(x, y - randomPixels);
                    case DOWN -> robot.mouseMove(x, y + randomPixels);
                    case LEFT -> robot.mouseMove(x - randomPixels, y);
                    case RIGHT -> robot.mouseMove(x + randomPixels, y);
                }

                Scheduler.schedule(() -> robot.mouseMove(stoppedPoint.x, stoppedPoint.y), POINTER_MOVE_DELAY);
            }
        }

        private enum Direction {
            UP, DOWN, LEFT, RIGHT
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
