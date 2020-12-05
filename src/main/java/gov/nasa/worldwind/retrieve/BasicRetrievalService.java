/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.*;

import javax.net.ssl.SSLHandshakeException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Performs threaded retrieval of data.
 *
 * @author Tom Gaskins
 * @version $Id: BasicRetrievalService.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public final class BasicRetrievalService extends WWObjectImpl
    implements RetrievalService, Thread.UncaughtExceptionHandler {
    // These constants are last-ditch values in case Configuration lacks defaults
    private static final int DEFAULT_QUEUE_SIZE = 1024;
    private static final int DEFAULT_POOL_SIZE = 8;
    private static final long DEFAULT_STALE_REQUEST_LIMIT = 30000; // milliseconds
    static final int DEFAULT_TIME_PRIORITY_GRANULARITY = 500; // milliseconds

private static final String IDLE_THREAD_NAME_PREFIX = Logging.getMessage(
        "BasicRetrievalService.IdleThreadNamePrefix");

    private final RetrievalExecutor executor; // thread pool for running retrievers
    private final Map<String,RetrievalTask> activeTasks; // tasks currently allocated a thread
    private final int queueSize; // maximum queue size
    protected SSLExceptionListener sslExceptionListener;

    public BasicRetrievalService() {
        Integer poolSize = Configuration.getIntegerValue(AVKey.RETRIEVAL_POOL_SIZE, DEFAULT_POOL_SIZE);
        this.queueSize = Configuration.getIntegerValue(AVKey.RETRIEVAL_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);

        // this.executor runs the retrievers, each in their own thread
        this.executor = new RetrievalExecutor(poolSize, this.queueSize);

        // this.activeTasks holds the list of currently executing tasks (*not* those pending on the queue)
        this.activeTasks = new ConcurrentHashMap();
    }

    public SSLExceptionListener getSSLExceptionListener() {
        return sslExceptionListener;
    }

    public void setSSLExceptionListener(SSLExceptionListener sslExceptionListener) {
        this.sslExceptionListener = sslExceptionListener;
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        Logging.logger().fine(Logging.getMessage("BasicRetrievalService.UncaughtExceptionDuringRetrieval",
            thread.getName()));
    }

    public void shutdown(boolean immediately) {
        if (immediately)
            this.executor.shutdownNow();
        else
            this.executor.shutdown();

        activeTasks.clear();
    }


    /**
     * @param retrieval the retriever to run
     * @param priority  the secondary priority of the retriever, or negative if it is to be the primary priority
     * @return a future object that can be used to query the request status of cancel the request.
     * @throws IllegalArgumentException if <code>retriever</code> is null or has no name
     */
    public synchronized RetrievalFuture run(Retriever retrieval, double priority) {

        if (!this.isAvailable()) {
            Logging.logger().finer(Logging.getMessage("BasicRetrievalService.ResourceRejected", retrieval.getName()));
            return null;
        }

        RetrievalTask x = new RetrievalTask(retrieval, priority);

        RetrievalTask X = activeTasks.compute(x.name, (n,p) -> {
            if (p!=null) {
                if (p!=x) {
                    p.priority = Math.max(p.priority, x.priority);
                    p.retriever.setSubmitEpochNow(); //boost existing
                    x.cancel(false); //already queued
                }
                return p;
            } else {
                return x;
            }
        });
        if (X==x) {
            X.retriever.setSubmitEpochNow();
            executor.execute(X);
        }
        return X;
    }

    public int getRetrieverPoolSize() {
        return this.executor.getCorePoolSize();
    }

    /**
     * @param poolSize the number of threads in the thread pool
     * @throws IllegalArgumentException if <code>poolSize</code> is non-positive
     */
    public void setRetrieverPoolSize(int poolSize) {
        if (poolSize < 1) {
            String message = Logging.getMessage("BasicRetrievalService.RetrieverPoolSizeIsLessThanOne");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        this.executor.setCorePoolSize(poolSize);
        this.executor.setMaximumPoolSize(poolSize);
    }

    public boolean hasActiveTasks() {
        return executor.getActiveCount()>0;
    }

    public boolean isAvailable() {
        return true;
        //return this.executor.getQueue().size() < this.queueSize;
//            && !WorldWind.getNetworkStatus().isNetworkUnavailable();
    }



    public double getProgress() {
        int totalContentLength = 0;
        int totalBytesRead = 0;

        for (RetrievalTask task : this.activeTasks.values()) {
            if (task.isDone())
                continue;

            Retriever retriever = task.getRetriever();
            try {
                double tcl = retriever.getContentLength();
                if (tcl > 0) {
                    totalContentLength += tcl;
                    totalBytesRead += retriever.getContentLengthRead();
                }
            }
            catch (Exception e) {
                Logging.logger().log(Level.FINE,
                    Logging.getMessage("BasicRetrievalService.ExceptionRetrievingContentSizes",
                        retriever.getName() != null ? retriever.getName() : ""), e);
            }
        }

        for (Runnable runnable : this.executor.getQueue()) {
            RetrievalFuture task =
                (RetrievalFuture) runnable;

            Retriever retriever = task.getRetriever();
            try {
                double tcl = retriever.getContentLength();
                if (tcl > 0) {
                    totalContentLength += tcl;
                    totalBytesRead += retriever.getContentLengthRead();
                }
            }
            catch (Exception e) {
                String message = Logging.getMessage("BasicRetrievalService.ExceptionRetrievingContentSizes") + (
                    retriever.getName() != null ? retriever.getName() : "");
                Logging.logger().log(Level.FINE, message, e);
            }
        }

        // Compute an aggregated progress notification.

        double progress;

        if (totalContentLength < 1)
            progress = 0;
        else
            progress = Math.min(100.0, 100.0 * totalBytesRead / totalContentLength);

        return progress;
    }

    /**
     * Encapsulates a single threaded retrieval as a {@link FutureTask}.
     */
    private static class RetrievalTask extends FutureTask<Retriever>
        implements RetrievalFuture, Comparable<RetrievalTask> {
        private final Retriever retriever;
        private double priority; // retrieval secondary priority (primary priority is submit time)
        private final String name;
        private final int hash;

        private RetrievalTask(Retriever retriever, double priority) {
            super(retriever);
            this.retriever = retriever;
            this.priority = priority;
            this.name = retriever.getName();
            this.hash = retriever.hashCode();
        }

        public double getPriority() {
            return priority;
        }

        public Retriever getRetriever() {
            return this.retriever;
        }

        @Override
        public void run() {
            if (this.isDone() || this.isCancelled())
                return;

            super.run();
        }

        /**
         * @param that the task to compare with this one
         * @return 0 if task priorities are equal, -1 if priority of this is less than that, 1 otherwise
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RetrievalTask that) {
            if (this==that) return 0;


//            if (this.priority > 0 == that.priority > 0) {
//                // Requests submitted within different time-granularity periods are ordered exclusive of their
//                // client-specified priority.
//                final long dSubmit = retriever.getSubmitEpoch() - that.retriever.getSubmitEpoch();
//                if (dSubmit!=0)
//                    return dSubmit > 0 ? -1 : +1; //prefer the newer task
//            }
//
//            // The client-specified priority is compared for requests submitted within the same granularity period.
//            int dp = Double.compare(that.priority, this.priority);

            int dp = Double.compare(
                that.priority + that.retriever.getSubmitEpoch(),
                this.priority + this.retriever.getSubmitEpoch());
            return dp!=0 ? dp : Integer.compare(System.identityHashCode(that), System.identityHashCode(this));
        }

        public final boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            // Tasks are equal if their retrievers are equivalent
            final RetrievalTask r = (RetrievalTask) o;
            return hash==r.hash && name.equals(r.name);
            // Priority and submit time are not factors in equality
        }

        public final int hashCode() {
            return hash;
        }


    }

    private class RetrievalExecutor extends ThreadPoolExecutor {
        private static final long THREAD_TIMEOUT = 2; // keep idle threads alive this many seconds
        private final long staleRequestLimit; // reject requests older than this

        private RetrievalExecutor(int poolSize, int queueSize) {
            super(poolSize, poolSize, THREAD_TIMEOUT, TimeUnit.SECONDS, new PriorityBlockingQueue<>(queueSize),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    thread.setUncaughtExceptionHandler(BasicRetrievalService.this);
                    return thread;
                }, new ThreadPoolExecutor.DiscardPolicy() {// abandon task when queue is full
                    // This listener is invoked only when the executor queue is a bounded queue and runs out of room.
                    // If the queue is a java.util.concurrent.PriorityBlockingQueue, this listener is never invoked.
                    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
                        // Interposes logging for rejected execution
                        Logging.logger().finer(Logging.getMessage("BasicRetrievalService.ResourceRejected",
                            ((RetrievalFuture) runnable).getRetriever().getName()));

                        super.rejectedExecution(runnable, threadPoolExecutor);
                    }
                });

            this.staleRequestLimit = Configuration.getLongValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT,
                DEFAULT_STALE_REQUEST_LIMIT);
        }

        /**
         * @param thread   the thread the task is running on
         * @param runnable the <code>Retriever</code> running on the thread
         * @throws IllegalArgumentException if either <code>thread</code> or <code>runnable</code> is null
         */
        protected void beforeExecute(Thread thread, Runnable runnable) {

            RetrievalTask task = (RetrievalTask) runnable;


//            task.retriever.setBeginTime(System.currentTimeMillis());
//            long limit = task.retriever.getStaleRequestLimit() >= 0
//                ? task.retriever.getStaleRequestLimit() : this.staleRequestLimit;
//            if (task.retriever.getBeginTime() - task.retriever.getSubmitTime() > limit) {
//                // Task has been sitting on the queue too long
//                Logging.logger().finer(Logging.getMessage("BasicRetrievalService.CancellingTooOldRetrieval",
//                    task.getRetriever().getName()));
//                task.cancel(true);
//            } else if (!BasicRetrievalService.this.activeTasks.add(task)) {
//                // Task is a duplicate
//                Logging.logger().finer(Logging.getMessage("BasicRetrievalService.CancellingDuplicateRetrieval",
//                    task.getRetriever().getName()));
//                task.cancel(true);
//            } else {

                thread.setName(task.name);
                thread.setPriority(Thread.MIN_PRIORITY); // Subordinate thread priority to rendering
                thread.setUncaughtExceptionHandler(BasicRetrievalService.this);

                super.beforeExecute(thread, runnable);
//            }
        }

        /**
         * @param runnable  the <code>Retriever</code> running on the thread
         * @param throwable an exception thrown during retrieval, will be null if no exception occurred
         * @throws IllegalArgumentException if <code>runnable</code> is null
         */
        protected void afterExecute(Runnable runnable, Throwable throwable) {

            RetrievalTask task = (RetrievalTask) runnable;

            boolean removed = BasicRetrievalService.this.activeTasks.remove(task.name)!=null;
            if (!removed) throw new RuntimeException();

//            task.retriever.setEndTime(System.currentTimeMillis());

            super.afterExecute(runnable, throwable);

            try {
                if (throwable != null) {
                    Logging.logger().log(Level.FINE,
                        Logging.getMessage("BasicRetrievalService.ExceptionDuringRetrieval",
                            task.getRetriever().getName()), throwable);
                }

                task.get(); // Wait for task to finish, cancel or break
            }
            catch (ExecutionException e) {
                String message = Logging.getMessage("BasicRetrievalService.ExecutionExceptionDuringRetrieval",
                    task.getRetriever().getName());
                final Throwable cause = e.getCause();
                if (cause instanceof SocketTimeoutException || cause instanceof ConnectException) {
                    Logging.logger().fine(message + " " + cause.getLocalizedMessage());
                }
                else if (cause instanceof SSLHandshakeException) {
                    if (sslExceptionListener != null)
                        sslExceptionListener.onException(cause, task.getRetriever().getName());
                    else
                        Logging.logger().fine(message + " " + cause.getLocalizedMessage());
                }
                else {
                    Logging.logger().log(Level.FINE, message, e);
                }
            }
            catch (InterruptedException e) {
                Logging.logger().log(Level.FINE, Logging.getMessage("BasicRetrievalService.RetrievalInterrupted",
                    task.getRetriever().getName()), e);
            }
            catch (CancellationException e) {
                Logging.logger().fine(Logging.getMessage("BasicRetrievalService.RetrievalCancelled",
                    task.getRetriever().getName()));
            }
            finally {
                Thread.currentThread().setName(IDLE_THREAD_NAME_PREFIX);
            }
        }
    }
}
