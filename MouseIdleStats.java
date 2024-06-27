import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MouseIdleStats {

    public static void main(String[] args) {
        try {
            boolean keepOsAlive = args.length > 0 && args[0].equals("--keep-os-alive");
            final MouseManager mouseManager = new MouseManager(keepOsAlive);
            ExecutorService service = Executors.newCachedThreadPool();
            service.submit(mouseManager);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    mouseManager.stop();
                    service.awaitTermination(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private static class MouseManager implements Runnable {

        private static final Duration CHECKING_INTERVAL = Duration.ofMinutes(2);
        private static final Duration COUNTER_DELAY = Duration.ofSeconds(1);

        private Point lastCoordinate;
        private LocalDateTime currentDateTime;
        private LocalDateTime lastMovement;
        private Duration idleTotalTime = Duration.ZERO;

        private final boolean keepOsAlive;
        private final PointerMover pointerMover;

        private volatile boolean running = true;

        public MouseManager(boolean keepOsAlive) throws AWTException {
            this.keepOsAlive = keepOsAlive;
            this.pointerMover = new PointerMover();
        }

        @Override
        public void run() {
            try {
                start();
            } catch (InterruptedException | AWTException e) {
                throw new RuntimeException(e);
            }
        }

        public void start() throws InterruptedException, AWTException {
            log("Started (checking for %d min of idle time)", CHECKING_INTERVAL.toMinutes());
            if (keepOsAlive)
                log("Keep OS alive during mouse idle checking");

            while (running) {
                Point currentPoint = MouseInfo.getPointerInfo().getLocation();
                currentDateTime = LocalDateTime.now();
                if (currentPoint.equals(lastCoordinate)) {
                    startTimeCounter(currentPoint);
                } else {
                    waitForNextCheck();
                }
                lastCoordinate = new Point(currentPoint);
            }
        }

        public void stop() {
            running = false;
            checkLastMovement();
            log("Done");
        }

        private void startTimeCounter(Point currentPoint) throws InterruptedException {
            if (lastMovement == null) {
                log("Starting counting idle time...");
                lastMovement = currentDateTime;
            }
            if (keepOsAlive) {
                pointerMover.move(currentPoint);
            }

            Thread.sleep(COUNTER_DELAY.toMillis());
        }

        private void waitForNextCheck() throws InterruptedException {
            checkLastMovement();
            Thread.sleep(CHECKING_INTERVAL.toMillis());
        }

        private void checkLastMovement() {
            if (lastMovement != null) {
                log("End: %s", resolveTimeDifference(lastMovement, currentDateTime));
                log("Total mouse idle time: %s", format(idleTotalTime));
                lastMovement = null;
            }
        }

        private String resolveTimeDifference(LocalDateTime dateTime1, LocalDateTime dateTime2) {
            Duration diff = Duration.between(dateTime1, dateTime2).plus(CHECKING_INTERVAL);
            idleTotalTime = idleTotalTime.plus(diff);
            return format(diff);
        }

        public String format(Duration duration) {
            return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
        }

        private void log(String message, Object... properties) {
            LocalDateTime dateTime = lastMovement != null ? lastMovement : LocalDateTime.now();
            String date = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            String formatted = String.format(message, properties);
            System.out.println(date + " " + formatted);
        }
    }

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private static class PointerMover {

        private static final int POINTER_MOVE_DELAY = 100; // in milliseconds

        private final Robot robot;

        public PointerMover() throws AWTException {
            this.robot = new Robot();
        }

        public void move(Point currentPoint) throws InterruptedException {
            Direction randomDirection = Direction.values()[new Random().nextInt(4)];
            int randomPixels = new Random().nextInt(90) + 10;
            int x = currentPoint.x;
            int y = currentPoint.y;

            switch (randomDirection) {
                case UP -> robot.mouseMove(x, y - randomPixels);
                case DOWN -> robot.mouseMove(x, y + randomPixels);
                case LEFT -> robot.mouseMove(x - randomPixels, y);
                case RIGHT -> robot.mouseMove(x + randomPixels, y);
            }

            Thread.sleep(POINTER_MOVE_DELAY);
            robot.mouseMove(currentPoint.x, currentPoint.y);
        }
    }
}
