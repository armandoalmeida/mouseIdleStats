package br.com.armandoalmeida.pointer;

import br.com.armandoalmeida.util.CustomLogger;
import br.com.armandoalmeida.util.Scheduler;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;

public class MouseManager implements Runnable {

    private static final CustomLogger log = CustomLogger.getLogger(Level.INFO, MouseManager.class.getName());

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
