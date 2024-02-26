import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MouseMover {

    public static void main(String[] args) {
        try {
            boolean keepOsAlive = args.length > 0 && args[0].equals("--keep-os-alive");
            new MouseManager(keepOsAlive).start();
        } catch (AWTException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("java:S2189")
    private static class MouseManager {

        private static final int CHECK_INTERVAL_IN_MILLIS = (1000 * 60) * 4;

        private long idleTotalTime = 0;
        private Coordinate lastCoordinate = null;

        private final boolean keepOsAlive;

        public MouseManager(boolean keepOsAlive) {
            this.keepOsAlive = keepOsAlive;
        }

        public void start() throws InterruptedException, AWTException {
            log("started (checking for %d min of idle time)", resolveCheckIntervalInMinutes());

            LocalDateTime lastMovement = null;
            while (true) {
                int x = (int) MouseInfo.getPointerInfo().getLocation().getX();
                int y = (int) MouseInfo.getPointerInfo().getLocation().getY();
                Coordinate currentCoordinate = new Coordinate(x, y);

                // System.out.println(lastCoordinate + " -> " + currentCoordinate);
                if (currentCoordinate.equals(lastCoordinate)) {
                    if (lastMovement == null) {
                        log("starting counting idle time...");
                        lastMovement = currentCoordinate.date;
                    }

                    if (keepOsAlive)
                        moveMousePointer(currentCoordinate);

                    Thread.sleep(500);
                } else {
                    if (lastMovement != null) {
                        log("end: %s", resolveTimeDifference(lastMovement, currentCoordinate.date));
                        log("total mouse idle time: %s", formatSeconds(idleTotalTime));
                        lastMovement = null;
                    }
                    Thread.sleep(CHECK_INTERVAL_IN_MILLIS);
                }
                lastCoordinate = currentCoordinate.clone();
            }
        }

        private static Integer resolveCheckIntervalInMinutes() {
            LocalDateTime checkInterval = LocalDateTime.ofInstant(Instant.ofEpochMilli(CHECK_INTERVAL_IN_MILLIS), ZoneId.systemDefault());
            return Integer.valueOf(DateTimeFormatter.ofPattern("m").format(checkInterval));
        }

        /**
         * Keeps the OS alive
         *
         * @param currentCoordinate keep the pointer in the same coordinate
         * @throws AWTException         due to mouse movement with {@link Robot}
         * @throws InterruptedException due to {@link Thread#sleep(long)}
         */
        private void moveMousePointer(Coordinate currentCoordinate) throws AWTException, InterruptedException {
            Robot robot = new Robot();
            robot.mouseMove(currentCoordinate.x + 1, currentCoordinate.y);
            Thread.sleep(10);
            robot.mouseMove(currentCoordinate.x, currentCoordinate.y);
        }

        private String resolveTimeDifference(LocalDateTime dateTime1, LocalDateTime dateTime2) {
            long diffInSeconds = ChronoUnit.SECONDS.between(dateTime1, dateTime2) + (CHECK_INTERVAL_IN_MILLIS / 1000);
            idleTotalTime += diffInSeconds;
            return formatSeconds(diffInSeconds);
        }

        public String formatSeconds(long seconds) {
            Duration duration = Duration.ofSeconds(seconds);
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
