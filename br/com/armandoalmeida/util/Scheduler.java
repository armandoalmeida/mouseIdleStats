package br.com.armandoalmeida.util;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Scheduler implements Runnable {

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
