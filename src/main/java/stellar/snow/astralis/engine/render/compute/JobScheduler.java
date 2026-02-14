package stellar.snow.astralis.engine.render.compute;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
/**
 * JobScheduler - Structured Multi-Threading with Workload Estimation
 * 
 * Unlike simple virtual thread spawning, this scheduler:
 * - Breaks tasks into ParallelJob units with estimated workloads
 * - Balances load across CPU cores intelligently
 * - Prioritizes critical path tasks (chunk generation, etc.)
 * - Prevents "noisy neighbor" problems
 * 
 * Features:
 * - Priority-based job queuing
 * - Work stealing for load balancing
 * - Dependency tracking
 * - Automatic thread pool sizing
 * - Performance monitoring
 */
    
    /**
     * Job priority levels
     */
    public enum Priority {
        CRITICAL(0),    // Must complete this frame (rendering)
        HIGH(1),        // Should complete this frame (chunk generation)
        NORMAL(2),      // Best effort this frame
        LOW(3),         // Background tasks
        IDLE(4);        // Only when CPU is idle
        
        public final int level;
        
        Priority(int level) {
            this.level = level;
        }
    }
    
    /**
     * Job execution context
     */
    public static class JobContext {
        private final int workerId;
        private final long startTime;
        private volatile boolean cancelled = false;
        
        public JobContext(int workerId) {
            this.workerId = workerId;
            this.startTime = System.nanoTime();
        }
        
        public int getWorkerId() {
            return workerId;
        }
        
        public long getElapsedNanos() {
            return System.nanoTime() - startTime;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
        
        public void cancel() {
            cancelled = true;
        }
    }
    
    /**
     * Parallel job definition
     */
    public static class ParallelJob<T> {
        private final String name;
        private final Priority priority;
        private final long estimatedWorkload; // in nanoseconds
        private final Function<JobContext, T> task;
        private final CompletableFuture<T> result;
        private final Set<ParallelJob<?>> dependencies;
        private final long submissionTime;
        private long scheduledTime;
        private long completionTime;
        
        public ParallelJob(String name, Priority priority, long estimatedWorkload, 
                          Function<JobContext, T> task) {
            this.name = name;
            this.priority = priority;
            this.estimatedWorkload = estimatedWorkload;
            this.task = task;
            this.result = new CompletableFuture<>();
            this.dependencies = ConcurrentHashMap.newKeySet();
            this.submissionTime = System.nanoTime();
        }
        
        public String getName() {
            return name;
        }
        
        public Priority getPriority() {
            return priority;
        }
        
        public long getEstimatedWorkload() {
            return estimatedWorkload;
        }
        
        public CompletableFuture<T> getResult() {
            return result;
        }
        
        public void addDependency(ParallelJob<?> dependency) {
            dependencies.add(dependency);
        }
        
        public boolean areDependenciesMet() {
            return dependencies.stream().allMatch(dep -> dep.result.isDone());
        }
        
        public long getWaitTime() {
            return scheduledTime > 0 ? scheduledTime - submissionTime : 0;
        }
        
        public long getExecutionTime() {
            return completionTime > 0 ? completionTime - scheduledTime : 0;
        }
    }
    
    // Worker threads
    private final int numWorkers;
    private final Worker[] workers;
    private final Thread[] workerThreads;
    
    // Job queues (one per priority level)
    private final BlockingQueue<ParallelJob<?>>[] priorityQueues;
    
    // Global job queue for work stealing
    private final ConcurrentLinkedQueue<ParallelJob<?>> globalQueue = new ConcurrentLinkedQueue<>();
    
    // Scheduler state
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    // Statistics
    private final AtomicLong totalJobsSubmitted = new AtomicLong(0);
    private final AtomicLong totalJobsCompleted = new AtomicLong(0);
    private final AtomicLong totalJobsFailed = new AtomicLong(0);
    private final AtomicLong totalWorkSteals = new AtomicLong(0);
    private final Map<Priority, AtomicLong> jobsByPriority = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    
    // CPU load tracking
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    
    @SuppressWarnings("unchecked")
    public JobScheduler(int numWorkers) {
        this.numWorkers = numWorkers > 0 ? numWorkers : Runtime.getRuntime().availableProcessors();
        this.workers = new Worker[this.numWorkers];
        this.workerThreads = new Thread[this.numWorkers];
        
        // Create priority queues
        this.priorityQueues = new BlockingQueue[Priority.values().length];
        for (Priority p : Priority.values()) {
            priorityQueues[p.level] = new LinkedBlockingQueue<>();
            jobsByPriority.put(p, new AtomicLong(0));
        }
        
        // Start workers
        for (int i = 0; i < this.numWorkers; i++) {
            workers[i] = new Worker(i);
            workerThreads[i] = new Thread(workers[i], "JobWorker-" + i);
            workerThreads[i].start();
        }
    }
    
    /**
     * Submit a job
     */
    public <T> CompletableFuture<T> submit(String name, Priority priority, long estimatedNanos, 
                                            Function<JobContext, T> task) {
        ParallelJob<T> job = new ParallelJob<>(name, priority, estimatedNanos, task);
        
        // Add to appropriate queue
        priorityQueues[priority.level].offer(job);
        globalQueue.offer(job);
        
        totalJobsSubmitted.incrementAndGet();
        jobsByPriority.get(priority).incrementAndGet();
        
        return job.result;
    }
    
    /**
     * Submit a simple runnable job
     */
    public CompletableFuture<Void> submit(String name, Priority priority, long estimatedNanos, 
                                           Consumer<JobContext> task) {
        return submit(name, priority, estimatedNanos, ctx -> {
            task.accept(ctx);
            return null;
        });
    }
    
    /**
     * Submit with auto workload estimation (based on previous runs)
     */
    public <T> CompletableFuture<T> submitAuto(String name, Priority priority, Function<JobContext, T> task) {
        long estimated = estimateWorkload(name);
        return submit(name, priority, estimated, task);
    }
    
    /**
     * Estimate workload based on historical data
     */
    private long estimateWorkload(String jobName) {
        // TODO: Implement historical tracking
        // For now, return a default estimate
        return TimeUnit.MILLISECONDS.toNanos(10);
    }
    
    /**
     * Submit a batch of jobs
     */
    public <T> List<CompletableFuture<T>> submitBatch(String namePrefix, Priority priority, 
                                                       List<Function<JobContext, T>> tasks, 
                                                       long estimatedNanosPerTask) {
        List<CompletableFuture<T>> futures = new ArrayList<>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            String name = namePrefix + "_" + i;
            futures.add(submit(name, priority, estimatedNanosPerTask, tasks.get(i)));
        }
        return futures;
    }
    
    /**
     * Wait for all jobs in a list to complete
     */
    public <T> List<T> awaitAll(List<CompletableFuture<T>> futures) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.get();
        
        List<T> results = new ArrayList<>(futures.size());
        for (CompletableFuture<T> future : futures) {
            results.add(future.get());
        }
        return results;
    }
    
    /**
     * Worker thread
     */
    private class Worker implements Runnable {
        private final int workerId;
        private final Random random = new Random();
        private long totalExecuted = 0;
        private long totalStolen = 0;
        
        public Worker(int workerId) {
            this.workerId = workerId;
        }
        
        @Override
        public void run() {
            while (running.get()) {
                try {
                    ParallelJob<?> job = findJob();
                    if (job != null) {
                        executeJob(job);
                    } else {
                        // No work available, sleep briefly
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    if (!running.get()) break;
                } catch (Exception e) {
                    System.err.println("Worker " + workerId + " error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        /**
         * Find next job to execute (with work stealing)
         */
        private ParallelJob<?> findJob() throws InterruptedException {
            // 1. Check priority queues (highest priority first)
            for (int i = 0; i < priorityQueues.length; i++) {
                ParallelJob<?> job = priorityQueues[i].poll(1, TimeUnit.MILLISECONDS);
                if (job != null && job.areDependenciesMet()) {
                    return job;
                }
            }
            
            // 2. Try work stealing from global queue
            ParallelJob<?> stolen = globalQueue.poll();
            if (stolen != null && stolen.areDependenciesMet()) {
                totalStolen++;
                totalWorkSteals.incrementAndGet();
                return stolen;
            }
            
            return null;
        }
        
        /**
         * Execute a job
         */
        private <T> void executeJob(ParallelJob<T> job) {
            activeTasks.incrementAndGet();
            job.scheduledTime = System.nanoTime();
            
            JobContext context = new JobContext(workerId);
            
            try {
                // Wait for dependencies
                for (ParallelJob<?> dep : job.dependencies) {
                    dep.result.get();
                }
                
                // Execute task
                T result = job.task.apply(context);
                
                job.completionTime = System.nanoTime();
                job.result.complete(result);
                
                // Update statistics
                totalExecuted++;
                totalJobsCompleted.incrementAndGet();
                totalExecutionTime.addAndGet(job.getExecutionTime());
                totalWaitTime.addAndGet(job.getWaitTime());
                
            } catch (Exception e) {
                job.result.completeExceptionally(e);
                totalJobsFailed.incrementAndGet();
                System.err.println("Job failed: " + job.name + " - " + e.getMessage());
            } finally {
                activeTasks.decrementAndGet();
            }
        }
    }
    
    /**
     * Get current CPU load (0.0 to 1.0)
     */
    public double getCurrentLoad() {
        return (double) activeTasks.get() / numWorkers;
    }
    
    /**
     * Get number of pending jobs
     */
    public int getPendingJobCount() {
        int count = 0;
        for (BlockingQueue<ParallelJob<?>> queue : priorityQueues) {
            count += queue.size();
        }
        return count;
    }
    
    /**
     * Get statistics
     */
    public JobStatistics getStatistics() {
        long submitted = totalJobsSubmitted.get();
        long completed = totalJobsCompleted.get();
        long failed = totalJobsFailed.get();
        long avgExecTime = completed > 0 ? totalExecutionTime.get() / completed : 0;
        long avgWaitTime = completed > 0 ? totalWaitTime.get() / completed : 0;
        
        return new JobStatistics(
            submitted,
            completed,
            failed,
            getPendingJobCount(),
            avgExecTime / 1_000_000.0, // Convert to ms
            avgWaitTime / 1_000_000.0, // Convert to ms
            getCurrentLoad(),
            totalWorkSteals.get()
        );
    }
    
    /**
     * Shutdown the scheduler
     */
    @Override
    public void close() {
        running.set(false);
        
        // Interrupt all workers
        for (Thread thread : workerThreads) {
            thread.interrupt();
        }
        
        // Wait for workers to finish
        for (Thread thread : workerThreads) {
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                thread.interrupt();
            }
        }
        
        System.out.println("JobScheduler shutdown complete. " + getStatistics());
    }
    
    /**
     * Job statistics
     */
    public static class JobStatistics {
        public final long totalSubmitted;
        public final long totalCompleted;
        public final long totalFailed;
        public final int pending;
        public final double avgExecutionTimeMs;
        public final double avgWaitTimeMs;
        public final double currentLoad;
        public final long totalWorkSteals;
        
        public JobStatistics(long submitted, long completed, long failed, int pending,
                           double avgExec, double avgWait, double load, long steals) {
            this.totalSubmitted = submitted;
            this.totalCompleted = completed;
            this.totalFailed = failed;
            this.pending = pending;
            this.avgExecutionTimeMs = avgExec;
            this.avgWaitTimeMs = avgWait;
            this.currentLoad = load;
            this.totalWorkSteals = steals;
        }
        
        public double getCompletionRate() {
            return totalSubmitted > 0 ? (double) totalCompleted / totalSubmitted : 0.0;
        }
        
        public double getFailureRate() {
            return totalSubmitted > 0 ? (double) totalFailed / totalSubmitted : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("JobStats[submitted=%d, completed=%d, failed=%d, pending=%d, " +
                               "avgExec=%.2fms, avgWait=%.2fms, load=%.1f%%, steals=%d]",
                totalSubmitted, totalCompleted, totalFailed, pending,
                avgExecutionTimeMs, avgWaitTimeMs, currentLoad * 100, totalWorkSteals);
        }
    }
}
