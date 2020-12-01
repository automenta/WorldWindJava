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
 * @author Tom Gaskins
 * @version $Id: ThreadedTaskService.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class TaskService extends WWObjectImpl implements Thread.UncaughtExceptionHandler {
    static final private int DEFAULT_CORE_POOL_SIZE = 1;
    static final private int DEFAULT_QUEUE_SIZE = 10;
//    private static final String RUNNING_THREAD_NAME_PREFIX = Logging.getMessage(
//        "ThreadedTaskService.RunningThreadNamePrefix");
    private static final String IDLE_THREAD_NAME_PREFIX = Logging.getMessage(
        "ThreadedTaskService.IdleThreadNamePrefix");
    private final Set<Runnable> activeTasks; // tasks currently allocated a thread
    private final TaskExecutor executor; // thread pool for running retrievers

    public TaskService() {
        Integer poolSize = Configuration.getIntegerValue(AVKey.TASK_POOL_SIZE, DEFAULT_CORE_POOL_SIZE);
        Integer queueSize = Configuration.getIntegerValue(AVKey.TASK_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);

        // this.executor runs the tasks, each in their own thread
        this.executor = new TaskExecutor(poolSize, queueSize);

        // this.activeTasks holds the list of currently executing tasks
        this.activeTasks = Collections.newSetFromMap(new ConcurrentHashMap());
    }

    public void shutdown(boolean immediately) {
        if (immediately)
            this.executor.shutdownNow();
        else
            this.executor.shutdown();
        activeTasks.clear();
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        String message = Logging.getMessage("ThreadedTaskService.UncaughtExceptionDuringTask", thread.getName());
        Logging.logger().fine(message);
        Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
    }

    public synchronized boolean contains(Runnable runnable) {
        //noinspection SimplifiableIfStatement
        return runnable!=null && activeTasks.contains(runnable);
    }

    /**
     * Enqueues a task to run.
     *
     * @param runnable the task to add
     * @throws IllegalArgumentException if <code>runnable</code> is null
     */
    public void addTask(Runnable runnable) {
        if (this.activeTasks.add(runnable))
            this.executor.execute(runnable);
    }

    public boolean isFull() {
        return this.executor.getQueue().remainingCapacity() == 0;
    }

    public void drain(Queue<Runnable> requestQueue) {
        Runnable request;
        while (!isFull() && (request = requestQueue.poll()) != null) {
            addTask(request);
        }
    }

//    public boolean hasActiveTasks() {
//        Thread[] threads = new Thread[Thread.activeCount()];
//        int numThreads = Thread.enumerate(threads);
//        for (int i = 0; i < numThreads; i++) {
//            if (threads[i].getName().startsWith(RUNNING_THREAD_NAME_PREFIX))
//                return true;
//        }
//        return false;
//    }

    private class TaskExecutor extends ThreadPoolExecutor {
        private static final long THREAD_TIMEOUT = 2; // keep idle threads alive this many seconds

        private TaskExecutor(int poolSize, int queueSize) {
            super(poolSize, poolSize, THREAD_TIMEOUT, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    thread.setUncaughtExceptionHandler(TaskService.this);
                    return thread;
                },
                new ThreadPoolExecutor.DiscardPolicy() // abandon task when queue is full
                {
                    public void rejectedExecution(Runnable runnable,
                        ThreadPoolExecutor threadPoolExecutor) {
                        // Interposes logging for rejected execution
                        String message = Logging.getMessage("ThreadedTaskService.ResourceRejected", runnable);
                        Logging.logger().fine(message);
                        super.rejectedExecution(runnable, threadPoolExecutor);
                    }
                });
        }

        protected void beforeExecute(Thread thread, Runnable runnable) {
//            if (thread == null) {
//                String msg = Logging.getMessage("nullValue.ThreadIsNull");
//                Logging.logger().fine(msg);
//                throw new IllegalArgumentException(msg);
//            }
//
//            if (runnable == null) {
//                String msg = Logging.getMessage("nullValue.RunnableIsNull");
//                Logging.logger().fine(msg);
//                throw new IllegalArgumentException(msg);
//            }
////
//            if (ThreadedTaskService.this.activeTasks.contains(runnable)) {
//                // Duplicate requests are simply interrupted here. The task itself must check the thread's isInterrupted
//                // flag and actually terminate the task.
//                String message = Logging.getMessage("ThreadedTaskService.CancellingDuplicateTask", runnable);
//                Logging.logger().finer(message);
//                thread.interrupt();
//                return;
//            }
//
//            ThreadedTaskService.this.activeTasks.add(runnable);


            thread.setName(runnable.toString());
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setUncaughtExceptionHandler(TaskService.this);

            super.beforeExecute(thread, runnable);
        }

        protected void afterExecute(Runnable runnable, Throwable throwable) {
            boolean removed = activeTasks.remove(runnable);
            if (!removed) throw new RuntimeException();

            super.afterExecute(runnable, throwable);

            Thread.currentThread().setName(IDLE_THREAD_NAME_PREFIX);
        }
    }
}
