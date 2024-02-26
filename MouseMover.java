import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MouseMover {

    public static void main(String[] args) {
        try {
            boolean keepOsAlive = args.length > 0 && args[0].equals("--keep-os-alive");
            new MouseManager(keepOsAlive).start();
        } catch (AWTException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class MouseManager {

        private static final Duration CHECKING_INTERVAL = Duration.ofMinutes(4);
        private static final int MOUSE_MOVE_DELAY = 100; // in milliseconds

        private Coordinate lastCoordinate;
        private LocalDateTime lastMovement;
        private Duration idleTotalTime = Duration.ZERO;

        private final Robot robot;
        private final boolean keepOsAlive;

        private volatile boolean running = false;

        public MouseManager(boolean keepOsAlive) throws AWTException {
            this.robot = new Robot();
            this.keepOsAlive = keepOsAlive;
        }

        public void start() throws InterruptedException, AWTException {
            log("Started (checking for %d min of idle time)", CHECKING_INTERVAL.toMinutes());
            if (keepOsAlive)
                log("Keep OS alive during mouse idle checking");

            running = true;
            while (running) {
                Coordinate currentCoordinate = resolveCurrentCoordinate();
                if (currentCoordinate.equals(lastCoordinate)) {
                    startTimeCounter(currentCoordinate);
                } else {
                    waitForNextCheck(currentCoordinate);
                }
                lastCoordinate = currentCoordinate.clone();
            }
        }

        public void stop() {
            running = false;
        }

        private void startTimeCounter(Coordinate currentCoordinate) throws InterruptedException {
            if (lastMovement == null) {
                log("Starting counting idle time...");
                lastMovement = currentCoordinate.date;
            }

            if (keepOsAlive)
                moveMousePointer(currentCoordinate);

            Thread.sleep(Duration.ofSeconds(1).toMillis());
        }

        private void waitForNextCheck(Coordinate currentCoordinate) throws InterruptedException {
            if (lastMovement != null) {
                log("End: %s", resolveTimeDifference(lastMovement, currentCoordinate.date));
                log("Total mouse idle time: %s", format(idleTotalTime));
                lastMovement = null;
            }
            Thread.sleep(CHECKING_INTERVAL.toMillis());
        }

        private static Coordinate resolveCurrentCoordinate() {
            int x = (int) MouseInfo.getPointerInfo().getLocation().getX();
            int y = (int) MouseInfo.getPointerInfo().getLocation().getY();
            return new Coordinate(x, y);
        }

        private void moveMousePointer(Coordinate currentCoordinate) throws InterruptedException {
            robot.mouseMove(currentCoordinate.x + 1, currentCoordinate.y);
            Thread.sleep(MOUSE_MOVE_DELAY);
            robot.mouseMove(currentCoordinate.x, currentCoordinate.y);
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
            LocalDateTime dateTime = lastCoordinate != null ? lastCoordinate.date : LocalDateTime.now();
            String date = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            String formatted = String.format(message, properties);
            System.out.println(date + " " + formatted);
        }
    }

    private static class Coordinate {
        int x;
        int y;
        LocalDateTime date;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
            this.date = LocalDateTime.now();
        }

        @Override
        public Coordinate clone() {
            return new Coordinate(this.x, this.y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Coordinate that = (Coordinate) o;

            if (x != that.x) return false;
            return y == that.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

        @Override
        public String toString() {
            return "{" + "x=" + x + ", y=" + y + '}';
        }
    }
}
