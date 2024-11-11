package br.com.armandoalmeida.util;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The Scheduler class provides a mechanism to schedule tasks for execution
 * either periodically or as a one-time event. It implements the Runnable
 * interface, allowing it to be executed in a separate thread.
 *
 * <p>This class supports two types of scheduling:</p>
 * <ul>
 *     <li><strong>One-Shot:</strong> Executes the task once after a specified delay.</li>
 *     <li><strong>Periodic:</strong> Executes the task repeatedly at fixed intervals.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * Scheduler periodicScheduler = Scheduler.newPeriodic(() -> System.out.println("Periodic Task"), Duration.ofSeconds(5));
 * periodicScheduler.run();
 * // OR
 * new Thread(periodicScheduler).start();
 *
 * Scheduler.oneShot(() -> System.out.println("One-Shot Task"), Duration.ofSeconds(10));
 * </pre>
 */
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

    /**
     * Creates a new periodic scheduler that executes the specified command
     * at fixed intervals.
     *
     * @param command  the task to be executed
     * @param duration the duration between executions
     * @return a new Scheduler instance
     */
    public static Scheduler newPeriodic(Runnable command, Duration duration) {
        return new Scheduler(SchedulerType.PERIODIC, command, duration);
    }

    /**
     * Schedules a one-time execution of the specified command after the given duration.
     *
     * @param command  the task to be executed
     * @param duration the delay before execution
     */
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
        } else if (SchedulerType.PERIODIC.equals(schedulerType)) {
            this.scheduleAtFixedRate(command, duration);
        }
    }

    /**
     * Shuts down the scheduler, canceling any scheduled tasks and releasing resources.
     */
    public void shutdown() {
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(false);
        }
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
