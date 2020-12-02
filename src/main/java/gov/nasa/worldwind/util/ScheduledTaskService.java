/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;

import java.util.*;
import java.util.concurrent.*;

/**
 * A service to execute tasks periodically, or after a delay.
 *
 * @author pabercrombie
 * @version $Id: BasicScheduledTaskService.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ScheduledTaskService extends WWObjectImpl implements Thread.UncaughtExceptionHandler {
    /**
     * Default thread pool size.
     */
    protected static final int DEFAULT_POOL_SIZE = 1;
    /**
     * Name assigned to active threads.
     */
    protected static final String RUNNING_THREAD_NAME_PREFIX = Logging.getMessage(
        "ThreadedTaskService.RunningThreadNamePrefix");
    /**
     * Name assigned to idle threads.
     */
    protected static final String IDLE_THREAD_NAME_PREFIX = Logging.getMessage(
        "ThreadedTaskService.IdleThreadNamePrefix");

    /**
     * Tasks currently running.
     */
    protected final Set<Runnable> activeTasks;
    /**
     * Executor for running tasks.
     */
    protected final ScheduledTaskExecutor executor;

    /**
     * Create a new scheduled task service. The thread pool size is from the WorldWind configuration file property
     * {@link AVKey#TASK_POOL_SIZE}.
     */
    public ScheduledTaskService() {
        Integer poolSize = Configuration.getIntegerValue(AVKey.TASK_POOL_SIZE, DEFAULT_POOL_SIZE);

        // this.executor runs the tasks, each in their own thread
        this.executor = new ScheduledTaskExecutor(poolSize);

        // this.activeTasks holds the list of currently executing tasks
        this.activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public void shutdown(boolean immediately) {
        if (immediately)
            this.executor.shutdownNow();
        else
            this.executor.shutdown();

        this.activeTasks.clear();
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        String message = Logging.getMessage("ThreadedTaskService.UncaughtExceptionDuringTask", thread.getName());
        Logging.logger().fine(message);
        Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
    }

    /**
     * {@inheritDoc}
     */
    public void addTask(Runnable runnable) {

        if (!this.activeTasks.add(runnable))
            return;

        this.executor.execute(runnable);
    }

    /**
     * {@inheritDoc}
     */
    public ScheduledFuture<?> addScheduledTask(Runnable runnable, long delay, TimeUnit timeunit) {

        if (!this.activeTasks.add(runnable))
            return null;

        return this.executor.schedule(runnable, delay, timeunit);
    }

    /**
     * Custom executor to run tasks.
     */
    protected class ScheduledTaskExecutor extends ScheduledThreadPoolExecutor {
        protected ScheduledTaskExecutor(int poolSize) {
            super(poolSize,
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    thread.setUncaughtExceptionHandler(ScheduledTaskService.this);
                    return thread;
                },
                new DiscardPolicy() {
                    public void rejectedExecution(Runnable runnable, ScheduledThreadPoolExecutor threadPoolExecutor) {
                        // Interposes logging for rejected execution
                        String message = Logging.getMessage("ThreadedTaskService.ResourceRejected", runnable);
                        Logging.logger().fine(message);
                        super.rejectedExecution(runnable, threadPoolExecutor);
                    }
                });
        }

        @Override
        protected void beforeExecute(Thread thread, Runnable runnable) {

            thread.setName(RUNNING_THREAD_NAME_PREFIX + runnable);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setUncaughtExceptionHandler(ScheduledTaskService.this);

            super.beforeExecute(thread, runnable);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {

            ScheduledTaskService.this.activeTasks.remove(runnable);

            super.afterExecute(runnable, throwable);

            if (throwable == null)
                Thread.currentThread().setName(IDLE_THREAD_NAME_PREFIX);
        }
    }
}