/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;

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
@Deprecated public final class BasicRetrievalService extends WWObjectImpl
    implements RetrievalService, Thread.UncaughtExceptionHandler {
    static final int DEFAULT_TIME_PRIORITY_GRANULARITY = 1000; // milliseconds
    // These constants are last-ditch values in case Configuration lacks defaults
    private static final int DEFAULT_QUEUE_SIZE = 2048;
    private static final int DEFAULT_POOL_SIZE = 1;
//    private static final long DEFAULT_STALE_REQUEST_LIMIT = 30000; // milliseconds
    private static final String IDLE_THREAD_NAME_PREFIX = Logging.getMessage(
        "BasicRetrievalService.IdleThreadNamePrefix");

    private final RetrievalExecutor executor; // thread pool for running retrievers

    // this.activeTasks holds the list of currently executing tasks (*not* those pending on the queue)
    private final Map<String, RetrievalTask> activeTasks = new ConcurrentHashMap(); // tasks currently allocated a thread
    private final int queueSize; // maximum queue size
//    protected SSLExceptionListener sslExceptionListener;

    public BasicRetrievalService() {
        int poolSize = Configuration.getIntegerValue(AVKey.RETRIEVAL_POOL_SIZE,
            BasicRetrievalService.DEFAULT_POOL_SIZE);
        this.queueSize = Configuration.getIntegerValue(AVKey.RETRIEVAL_QUEUE_SIZE,
            BasicRetrievalService.DEFAULT_QUEUE_SIZE);

        // this.executor runs the retrievers, each in their own thread
        this.executor = new RetrievalExecutor(poolSize, this.queueSize);
    }

//    public SSLExceptionListener getSSLExceptionListener() {
//        return sslExceptionListener;
//    }
//
//    public void setSSLExceptionListener(SSLExceptionListener sslExceptionListener) {
//        this.sslExceptionListener = sslExceptionListener;
//    }

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

        assert(Double.isFinite(priority));

        if (!this.isAvailable()) {
            Logging.logger().finer(Logging.getMessage("BasicRetrievalService.ResourceRejected", retrieval.getName()));
            return null;
        }

        RetrievalTask x = new RetrievalTask(retrieval, priority);

        RetrievalTask X = activeTasks.compute(x.name, (n, p) -> {
            if (p != null) {
                if (p != x) {
                    p.priority = Math.max(p.priority, x.priority);
                    p.retriever.setSubmitEpochNow(); //boost existing
                    x.cancel(false); //already queued
                }
                return p;
            } else {
                return x;
            }
        });
        if (X == x) {
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
        return executor.getActiveCount() > 0;
    }

    public boolean isAvailable() {
        return true;
        //return this.executor.getQueue().size() < this.queueSize;
//            && !WorldWind.getNetworkStatus().isNetworkUnavailable();
    }

    /**
     * Encapsulates a single threaded retrieval as a {@link FutureTask}.
     */
    private static class RetrievalTask extends FutureTask<Retriever>
        implements RetrievalFuture, Comparable<RetrievalTask> {
        private final Retriever retriever;
        private final String name;
        private final int hash;
        private double priority; // retrieval secondary priority (primary priority is submit time)

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
            if (this == that)
                return 0;

            int dp = Double.compare(
                that.priority + that.retriever.getSubmitEpoch(),
                this.priority + this.retriever.getSubmitEpoch());
            return dp != 0 ? dp : Integer.compare(System.identityHashCode(that), System.identityHashCode(this));
        }

        public final boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            // Tasks are equal if their retrievers are equivalent
            final RetrievalTask r = (RetrievalTask) o;
            return hash == r.hash && name.equals(r.name);
            // Priority and submit time are not factors in equality
        }

        public final int hashCode() {
            return hash;
        }
    }

    private class RetrievalExecutor extends ThreadPoolExecutor {
        private static final long THREAD_TIMEOUT = 2; // keep idle threads alive this many seconds
//        private final long staleRequestLimit; // reject requests older than this

        private RetrievalExecutor(int poolSize, int queueSize) {
            super(poolSize, poolSize, RetrievalExecutor.THREAD_TIMEOUT, TimeUnit.SECONDS, new PriorityBlockingQueue<>(queueSize),
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

//            this.staleRequestLimit = Configuration.getLongValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT,
//                BasicRetrievalService.DEFAULT_STALE_REQUEST_LIMIT);
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

            boolean removed = BasicRetrievalService.this.activeTasks.remove(task.name) != null;
            if (!removed)
                throw new RuntimeException();

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
                    Logging.logger().fine(message + ' ' + cause.getLocalizedMessage());
                } else if (cause instanceof SSLHandshakeException) {
//                    if (sslExceptionListener != null)
//                        sslExceptionListener.onException(cause, task.getRetriever().getName());
//                    else
                    Logging.logger().fine(message + ' ' + cause.getLocalizedMessage());
                } else {
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
                Thread.currentThread().setName(BasicRetrievalService.IDLE_THREAD_NAME_PREFIX);
            }
        }
    }
}
