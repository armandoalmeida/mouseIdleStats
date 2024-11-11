package br.com.armandoalmeida.pointer;

import br.com.armandoalmeida.util.Scheduler;

import java.awt.*;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class PointerMover {
    private static final Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new IllegalStateException(e);
        }
    }

    public PointerMover() {
        throw new UnsupportedOperationException("Static class for pointer mover");
    }

    public static synchronized void move(Point stoppedPoint) {
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
