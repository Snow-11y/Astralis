package stellar.snow.astralis.engine.ecs.core;

import java.lang.annotation.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.*;

/**
 * System - High-performance ECS system base class with production-grade architecture.
 *
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li>Single Responsibility: System only handles logic, profiling/scheduling are separate</li>
 *   <li>No Global Singletons: All dependencies are injected</li>
 *   <li>Unlimited Components: Uses dynamic bitsets, not 64-bit masks</li>
 *   <li>Proper Parallelism: Custom job system, not ForkJoinPool</li>
 *   <li>Zero-Cost Abstractions: Profiling is opt-in</li>
 *   <li>Type-Safe Dependencies: Class references, not strings</li>
 * </ul>
 *
 * @author Enhanced ECS Framework
 * @version 3.0.0
 * @since Java 21
 */
public abstract class System implements AutoCloseable {

    // ========================================================================
    // ANNOTATIONS - Type-safe, compile-time checked
    // ========================================================================

    /**
     * Declare required components for this system.
     * Processed at registration time, not runtime.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(RequireComponents.List.class)
    public @interface RequireComponents {
        Class<?>[] value();
        AccessMode mode() default AccessMode.READ_WRITE;
        
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        @interface List {
            RequireComponents[] value();
        }
    }

    /**
     * Declare excluded components for this system.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ExcludeComponents {
        Class<?>[] value();
    }

    /**
     * Declare optional components (system runs with or without).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface OptionalComponents {
        Class<?>[] value();
    }

    /**
     * Declare system dependencies using Class references (type-safe).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DependsOn {
        Class<? extends System>[] value();
    }

    /**
     * Declare systems that must run AFTER this one.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RunBefore {
        Class<? extends System>[] value();
    }

    /**
     * Declare execution phase.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Phase {
        ExecutionPhase value();
    }

    /**
     * Declare system priority within phase.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Priority {
        int value() default 0;
    }

    /**
     * Mark system as thread-safe for parallel execution.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ThreadSafe {
        ParallelStrategy strategy() default ParallelStrategy.ARCHETYPES;
    }

    /**
     * Mark system as stateless (can be freely parallelized).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Stateless {}

    /**
     * Enable profiling for this system (opt-in).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Profiled {
        boolean detailed() default false;
    }

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Component access mode for dependency analysis.
     */
    public enum AccessMode {
        READ_ONLY,
        READ_WRITE,
        WRITE_ONLY
    }

    /**
     * System lifecycle state.
     */
    public enum State {
        CREATED,
        REGISTERED,
        INITIALIZING,
        READY,
        RUNNING,
        PAUSED,
        SUSPENDED,      // Temporarily disabled by scheduler
        SHUTTING_DOWN,
        SHUTDOWN,
        ERROR,
        DISPOSED
    }

    /**
     * Execution phase for system ordering.
     */
    public enum ExecutionPhase {
        PRE_UPDATE(0),
        EARLY_UPDATE(100),
        UPDATE(200),
        LATE_UPDATE(300),
        POST_UPDATE(400),
        PRE_RENDER(500),
        RENDER(600),
        POST_RENDER(700),
        CLEANUP(800);

        public final int order;
        ExecutionPhase(int order) { this.order = order; }
    }

    /**
     * Parallel execution strategy.
     */
    public enum ParallelStrategy {
        /** Sequential execution (default) */
        NONE,
        /** Parallelize across archetypes */
        ARCHETYPES,
        /** Parallelize across entities within each archetype */
        ENTITIES,
        /** Full parallelization (archetypes and entities) */
        FULL,
        /** Custom parallelization (system controls) */
        CUSTOM
    }

    // ========================================================================
    // COMPONENT MASK - Dynamic, unlimited components
    // ========================================================================

    /**
     * Dynamic component mask supporting unlimited component types.
     * Thread-safe and memory efficient.
     */
    public static final class ComponentMask implements Cloneable {
        
        private static final int BITS_PER_WORD = 64;
        private volatile long[] words;
        private final StampedLock lock = new StampedLock();
        
        // VarHandle for atomic operations on array elements
        private static final VarHandle WORDS_ARRAY;
        static {
            try {
                WORDS_ARRAY = MethodHandles.arrayElementVarHandle(long[].class);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public ComponentMask() {
            this.words = new long[4]; // Start with 256 component capacity
        }

        public ComponentMask(int initialCapacity) {
            int wordCount = (initialCapacity + BITS_PER_WORD - 1) / BITS_PER_WORD;
            this.words = new long[Math.max(1, wordCount)];
        }

        private ComponentMask(long[] words) {
            this.words = words.clone();
        }

        /**
         * Set a bit at the given index.
         */
        public void set(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("Index: " + index);
            
            int wordIndex = index / BITS_PER_WORD;
            ensureCapacity(wordIndex + 1);
            
            long bit = 1L << (index % BITS_PER_WORD);
            long stamp = lock.writeLock();
            try {
                words[wordIndex] |= bit;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        /**
         * Set a bit atomically (lock-free for existing capacity).
         */
        public void setAtomic(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("Index: " + index);
            
            int wordIndex = index / BITS_PER_WORD;
            
            // Check if we need to grow
            if (wordIndex >= words.length) {
                long stamp = lock.writeLock();
                try {
                    ensureCapacityLocked(wordIndex + 1);
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
            
            long bit = 1L << (index % BITS_PER_WORD);
            long[] w = words;
            long oldValue, newValue;
            do {
                oldValue = (long) WORDS_ARRAY.getVolatile(w, wordIndex);
                newValue = oldValue | bit;
            } while (!WORDS_ARRAY.compareAndSet(w, wordIndex, oldValue, newValue));
        }

        /**
         * Clear a bit at the given index.
         */
        public void clear(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("Index: " + index);
            
            int wordIndex = index / BITS_PER_WORD;
            if (wordIndex >= words.length) return;
            
            long bit = 1L << (index % BITS_PER_WORD);
            long stamp = lock.writeLock();
            try {
                words[wordIndex] &= ~bit;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        /**
         * Check if a bit is set.
         */
        public boolean get(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("Index: " + index);
            
            int wordIndex = index / BITS_PER_WORD;
            
            long stamp = lock.tryOptimisticRead();
            long[] w = words;
            if (wordIndex >= w.length) {
                if (!lock.validate(stamp)) {
                    stamp = lock.readLock();
                    try {
                        w = words;
                        if (wordIndex >= w.length) return false;
                    } finally {
                        lock.unlockRead(stamp);
                    }
                } else {
                    return false;
                }
            }
            
            long bit = 1L << (index % BITS_PER_WORD);
            boolean result = (w[wordIndex] & bit) != 0;
            
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    w = words;
                    if (wordIndex >= w.length) return false;
                    result = (w[wordIndex] & bit) != 0;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            
            return result;
        }

        /**
         * Check if this mask contains all bits from another mask.
         */
        public boolean containsAll(ComponentMask other) {
            long stamp = lock.tryOptimisticRead();
            long[] myWords = this.words;
            long[] otherWords = other.words;
            
            int minLen = Math.min(myWords.length, otherWords.length);
            
            // Check that all bits in other are present in this
            for (int i = 0; i < minLen; i++) {
                if ((myWords[i] & otherWords[i]) != otherWords[i]) {
                    if (!lock.validate(stamp)) {
                        return containsAllSlow(other);
                    }
                    return false;
                }
            }
            
            // Check remaining words in other are zero
            for (int i = minLen; i < otherWords.length; i++) {
                if (otherWords[i] != 0) {
                    if (!lock.validate(stamp)) {
                        return containsAllSlow(other);
                    }
                    return false;
                }
            }
            
            if (!lock.validate(stamp)) {
                return containsAllSlow(other);
            }
            
            return true;
        }

        private boolean containsAllSlow(ComponentMask other) {
            long stamp = lock.readLock();
            try {
                long[] myWords = this.words;
                long[] otherWords = other.words;
                
                int minLen = Math.min(myWords.length, otherWords.length);
                
                for (int i = 0; i < minLen; i++) {
                    if ((myWords[i] & otherWords[i]) != otherWords[i]) {
                        return false;
                    }
                }
                
                for (int i = minLen; i < otherWords.length; i++) {
                    if (otherWords[i] != 0) {
                        return false;
                    }
                }
                
                return true;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * Check if this mask intersects with another (any common bits).
         */
        public boolean intersects(ComponentMask other) {
            long stamp = lock.tryOptimisticRead();
            long[] myWords = this.words;
            long[] otherWords = other.words;
            
            int minLen = Math.min(myWords.length, otherWords.length);
            
            for (int i = 0; i < minLen; i++) {
                if ((myWords[i] & otherWords[i]) != 0) {
                    if (!lock.validate(stamp)) {
                        return intersectsSlow(other);
                    }
                    return true;
                }
            }
            
            if (!lock.validate(stamp)) {
                return intersectsSlow(other);
            }
            
            return false;
        }

        private boolean intersectsSlow(ComponentMask other) {
            long stamp = lock.readLock();
            try {
                long[] myWords = this.words;
                long[] otherWords = other.words;
                
                int minLen = Math.min(myWords.length, otherWords.length);
                
                for (int i = 0; i < minLen; i++) {
                    if ((myWords[i] & otherWords[i]) != 0) {
                        return true;
                    }
                }
                
                return false;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * Check if mask is empty.
         */
        public boolean isEmpty() {
            long stamp = lock.tryOptimisticRead();
            long[] w = words;
            
            for (long word : w) {
                if (word != 0) {
                    if (!lock.validate(stamp)) {
                        return isEmptySlow();
                    }
                    return false;
                }
            }
            
            if (!lock.validate(stamp)) {
                return isEmptySlow();
            }
            
            return true;
        }

        private boolean isEmptySlow() {
            long stamp = lock.readLock();
            try {
                for (long word : words) {
                    if (word != 0) return false;
                }
                return true;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * Count set bits.
         */
        public int cardinality() {
            long stamp = lock.readLock();
            try {
                int count = 0;
                for (long word : words) {
                    count += Long.bitCount(word);
                }
                return count;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * Get all set bit indices.
         */
        public int[] getSetBits() {
            long stamp = lock.readLock();
            try {
                int count = 0;
                for (long word : words) {
                    count += Long.bitCount(word);
                }
                
                int[] result = new int[count];
                int resultIndex = 0;
                
                for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
                    long word = words[wordIndex];
                    while (word != 0) {
                        int bit = Long.numberOfTrailingZeros(word);
                        result[resultIndex++] = wordIndex * BITS_PER_WORD + bit;
                        word &= ~(1L << bit);
                    }
                }
                
                return result;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * Iterate over set bits.
         */
        public void forEachSetBit(IntConsumer action) {
            long stamp = lock.readLock();
            try {
                for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
                    long word = words[wordIndex];
                    while (word != 0) {
                        int bit = Long.numberOfTrailingZeros(word);
                        action.accept(wordIndex * BITS_PER_WORD + bit);
                        word &= ~(1L << bit);
                    }
                }
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * Create union of two masks.
         */
        public ComponentMask or(ComponentMask other) {
            long stamp1 = this.lock.readLock();
            long stamp2 = other.lock.readLock();
            try {
                int maxLen = Math.max(this.words.length, other.words.length);
                long[] result = new long[maxLen];
                
                int minLen = Math.min(this.words.length, other.words.length);
                for (int i = 0; i < minLen; i++) {
                    result[i] = this.words[i] | other.words[i];
                }
                
                if (this.words.length > other.words.length) {
                    java.lang.System.arraycopy(this.words, minLen, result, minLen, 
                        this.words.length - minLen);
                } else if (other.words.length > this.words.length) {
                    java.lang.System.arraycopy(other.words, minLen, result, minLen,
                        other.words.length - minLen);
                }
                
                return new ComponentMask(result);
            } finally {
                other.lock.unlockRead(stamp2);
                this.lock.unlockRead(stamp1);
            }
        }

        /**
         * Create intersection of two masks.
         */
        public ComponentMask and(ComponentMask other) {
            long stamp1 = this.lock.readLock();
            long stamp2 = other.lock.readLock();
            try {
                int minLen = Math.min(this.words.length, other.words.length);
                long[] result = new long[minLen];
                
                for (int i = 0; i < minLen; i++) {
                    result[i] = this.words[i] & other.words[i];
                }
                
                return new ComponentMask(result);
            } finally {
                other.lock.unlockRead(stamp2);
                this.lock.unlockRead(stamp1);
            }
        }

        /**
         * Create difference (this AND NOT other).
         */
        public ComponentMask andNot(ComponentMask other) {
            long stamp1 = this.lock.readLock();
            long stamp2 = other.lock.readLock();
            try {
                long[] result = this.words.clone();
                int minLen = Math.min(result.length, other.words.length);
                
                for (int i = 0; i < minLen; i++) {
                    result[i] &= ~other.words[i];
                }
                
                return new ComponentMask(result);
            } finally {
                other.lock.unlockRead(stamp2);
                this.lock.unlockRead(stamp1);
            }
        }

        /**
         * Clear all bits.
         */
        public void clearAll() {
            long stamp = lock.writeLock();
            try {
                Arrays.fill(words, 0);
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        private void ensureCapacity(int wordCount) {
            if (wordCount <= words.length) return;
            
            long stamp = lock.writeLock();
            try {
                ensureCapacityLocked(wordCount);
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        private void ensureCapacityLocked(int wordCount) {
            if (wordCount <= words.length) return;
            
            int newSize = Math.max(wordCount, words.length * 2);
            long[] newWords = new long[newSize];
            java.lang.System.arraycopy(words, 0, newWords, 0, words.length);
            words = newWords;
        }

        /**
         * Get capacity in bits.
         */
        public int capacity() {
            return words.length * BITS_PER_WORD;
        }

        @Override
        public ComponentMask clone() {
            long stamp = lock.readLock();
            try {
                return new ComponentMask(words);
            } finally {
                lock.unlockRead(stamp);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ComponentMask other)) return false;
            
            long stamp1 = this.lock.readLock();
            long stamp2 = other.lock.readLock();
            try {
                int minLen = Math.min(this.words.length, other.words.length);
                
                for (int i = 0; i < minLen; i++) {
                    if (this.words[i] != other.words[i]) return false;
                }
                
                // Check that extra words are zero
                for (int i = minLen; i < this.words.length; i++) {
                    if (this.words[i] != 0) return false;
                }
                for (int i = minLen; i < other.words.length; i++) {
                    if (other.words[i] != 0) return false;
                }
                
                return true;
            } finally {
                other.lock.unlockRead(stamp2);
                this.lock.unlockRead(stamp1);
            }
        }

        @Override
        public int hashCode() {
            long stamp = lock.readLock();
            try {
                int result = 1;
                // Hash from the end to ignore trailing zeros
                int lastNonZero = words.length - 1;
                while (lastNonZero >= 0 && words[lastNonZero] == 0) {
                    lastNonZero--;
                }
                for (int i = 0; i <= lastNonZero; i++) {
                    long word = words[i];
                    result = 31 * result + (int)(word ^ (word >>> 32));
                }
                return result;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        @Override
        public String toString() {
            long stamp = lock.readLock();
            try {
                StringBuilder sb = new StringBuilder("ComponentMask{");
                boolean first = true;
                for (int i = 0; i < words.length; i++) {
                    long word = words[i];
                    while (word != 0) {
                        int bit = Long.numberOfTrailingZeros(word);
                        if (!first) sb.append(", ");
                        sb.append(i * BITS_PER_WORD + bit);
                        first = false;
                        word &= ~(1L << bit);
                    }
                }
                return sb.append("}").toString();
            } finally {
                lock.unlockRead(stamp);
            }
        }

        /**
         * Create mask from component IDs.
         */
        public static ComponentMask of(int... componentIds) {
            if (componentIds.length == 0) return new ComponentMask();
            
            int maxId = 0;
            for (int id : componentIds) {
                if (id > maxId) maxId = id;
            }
            
            ComponentMask mask = new ComponentMask(maxId + 1);
            for (int id : componentIds) {
                mask.set(id);
            }
            return mask;
        }

        /**
         * Create empty mask.
         */
        public static ComponentMask empty() {
            return new ComponentMask();
        }
    }

    // ========================================================================
    // JOB SYSTEM - Proper parallelization without ForkJoinPool
    // ========================================================================

    /**
     * Dedicated job system for ECS parallel execution.
     * Avoids the common ForkJoinPool used by parallelStream.
     */
    public static final class JobSystem {
        
        private static final int DEFAULT_THREAD_COUNT = 
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        
        private final ThreadPoolExecutor executor;
        private final BlockingQueue<Runnable> workQueue;
        private final AtomicLong jobsSubmitted = new AtomicLong();
        private final AtomicLong jobsCompleted = new AtomicLong();
        private final String name;
        private volatile boolean shutdown = false;
        
        // Thread-local for batching
        private final ThreadLocal<List<Runnable>> batchBuffer = 
            ThreadLocal.withInitial(() -> new ArrayList<>(64));

        public JobSystem(String name) {
            this(name, DEFAULT_THREAD_COUNT);
        }

        public JobSystem(String name, int threadCount) {
            this.name = name;
            this.workQueue = new LinkedBlockingQueue<>();
            
            ThreadFactory factory = new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger();
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, name + "-Worker-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    // Pin to core if possible (hint to OS)
                    t.setPriority(Thread.NORM_PRIORITY + 1);
                    return t;
                }
            };
            
            this.executor = new ThreadPoolExecutor(
                threadCount, threadCount,
                60L, TimeUnit.SECONDS,
                workQueue,
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
            
            // Pre-start all core threads
            executor.prestartAllCoreThreads();
        }

        /**
         * Submit a single job.
         */
        public CompletableFuture<Void> submit(Runnable job) {
            if (shutdown) {
                return CompletableFuture.failedFuture(
                    new RejectedExecutionException("JobSystem is shutdown"));
            }
            
            jobsSubmitted.incrementAndGet();
            
            return CompletableFuture.runAsync(() -> {
                try {
                    job.run();
                } finally {
                    jobsCompleted.incrementAndGet();
                }
            }, executor);
        }

        /**
         * Submit multiple jobs and wait for all to complete.
         */
        public void submitAndWait(List<Runnable> jobs) {
            if (jobs.isEmpty()) return;
            if (shutdown) throw new RejectedExecutionException("JobSystem is shutdown");
            
            int count = jobs.size();
            CountDownLatch latch = new CountDownLatch(count);
            jobsSubmitted.addAndGet(count);
            
            for (Runnable job : jobs) {
                executor.execute(() -> {
                    try {
                        job.run();
                    } finally {
                        jobsCompleted.incrementAndGet();
                        latch.countDown();
                    }
                });
            }
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Job execution interrupted", e);
            }
        }

        /**
         * Parallel for-each with optimal chunking.
         */
        public <T> void parallelForEach(List<T> items, Consumer<T> action) {
            if (items.isEmpty()) return;
            
            int threadCount = executor.getCorePoolSize();
            int itemCount = items.size();
            
            // If small enough, execute sequentially
            if (itemCount <= threadCount * 2) {
                for (T item : items) {
                    action.accept(item);
                }
                return;
            }
            
            int chunkSize = Math.max(1, (itemCount + threadCount - 1) / threadCount);
            int chunkCount = (itemCount + chunkSize - 1) / chunkSize;
            
            CountDownLatch latch = new CountDownLatch(chunkCount);
            jobsSubmitted.addAndGet(chunkCount);
            
            for (int i = 0; i < chunkCount; i++) {
                final int start = i * chunkSize;
                final int end = Math.min(start + chunkSize, itemCount);
                
                executor.execute(() -> {
                    try {
                        for (int j = start; j < end; j++) {
                            action.accept(items.get(j));
                        }
                    } finally {
                        jobsCompleted.incrementAndGet();
                        latch.countDown();
                    }
                });
            }
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel forEach interrupted", e);
            }
        }

        /**
         * Parallel for-each with indexed access.
         */
        public void parallelFor(int start, int end, IntConsumer action) {
            if (start >= end) return;
            
            int range = end - start;
            int threadCount = executor.getCorePoolSize();
            
            // If small enough, execute sequentially
            if (range <= threadCount * 4) {
                for (int i = start; i < end; i++) {
                    action.accept(i);
                }
                return;
            }
            
            int chunkSize = Math.max(1, (range + threadCount - 1) / threadCount);
            int chunkCount = (range + chunkSize - 1) / chunkSize;
            
            CountDownLatch latch = new CountDownLatch(chunkCount);
            jobsSubmitted.addAndGet(chunkCount);
            
            for (int c = 0; c < chunkCount; c++) {
                final int chunkStart = start + c * chunkSize;
                final int chunkEnd = Math.min(chunkStart + chunkSize, end);
                
                executor.execute(() -> {
                    try {
                        for (int i = chunkStart; i < chunkEnd; i++) {
                            action.accept(i);
                        }
                    } finally {
                        jobsCompleted.incrementAndGet();
                        latch.countDown();
                    }
                });
            }
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel for interrupted", e);
            }
        }

        /**
         * Get thread count.
         */
        public int getThreadCount() {
            return executor.getCorePoolSize();
        }

        /**
         * Get queue size.
         */
        public int getQueueSize() {
            return workQueue.size();
        }

        /**
         * Get active thread count.
         */
        public int getActiveCount() {
            return executor.getActiveCount();
        }

        /**
         * Get jobs submitted.
         */
        public long getJobsSubmitted() {
            return jobsSubmitted.get();
        }

        /**
         * Get jobs completed.
         */
        public long getJobsCompleted() {
            return jobsCompleted.get();
        }

        /**
         * Shutdown the job system.
         */
        public void shutdown() {
            shutdown = true;
            executor.shutdown();
        }

        /**
         * Shutdown and wait.
         */
        public void shutdownAndWait(long timeout, TimeUnit unit) throws InterruptedException {
            shutdown();
            executor.awaitTermination(timeout, unit);
        }

        /**
         * Force shutdown.
         */
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return executor.shutdownNow();
        }

        /**
         * Check if shutdown.
         */
        public boolean isShutdown() {
            return shutdown;
        }
    }

    // ========================================================================
    // PROFILER - Opt-in, zero-cost when disabled
    // ========================================================================

    /**
     * System profiler interface - allows for zero-cost abstraction.
     */
    public interface SystemProfiler {
        
        void onExecutionStart(System system);
        void onExecutionEnd(System system, long durationNanos, int entitiesProcessed);
        void onError(System system, Throwable error);
        void recordArchetype(System system, Archetype archetype);
        
        Metrics getMetrics(System system);
        void reset(System system);
        
        /**
         * No-op profiler for zero overhead.
         */
        SystemProfiler NOOP = new SystemProfiler() {
            @Override public void onExecutionStart(System system) {}
            @Override public void onExecutionEnd(System system, long durationNanos, int entitiesProcessed) {}
            @Override public void onError(System system, Throwable error) {}
            @Override public void recordArchetype(System system, Archetype archetype) {}
            @Override public Metrics getMetrics(System system) { return Metrics.EMPTY; }
            @Override public void reset(System system) {}
        };
    }

    /**
     * Default profiler implementation.
     */
    public static final class DefaultProfiler implements SystemProfiler {
        
        private final ConcurrentHashMap<UUID, ProfileData> profileData = new ConcurrentHashMap<>();
        
        private static final class ProfileData {
            final AtomicLong executionCount = new AtomicLong();
            final AtomicLong totalTimeNanos = new AtomicLong();
            final AtomicLong lastTimeNanos = new AtomicLong();
            final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
            final AtomicLong maxTimeNanos = new AtomicLong();
            final LongAdder totalEntities = new LongAdder();
            final LongAdder totalArchetypes = new LongAdder();
            final AtomicLong errorCount = new AtomicLong();
            volatile long startTime;
        }
        
        private ProfileData getOrCreate(System system) {
            return profileData.computeIfAbsent(system.id, k -> new ProfileData());
        }

        @Override
        public void onExecutionStart(System system) {
            ProfileData data = getOrCreate(system);
            data.startTime = java.lang.System.nanoTime();
        }

        @Override
        public void onExecutionEnd(System system, long durationNanos, int entitiesProcessed) {
            ProfileData data = getOrCreate(system);
            
            data.lastTimeNanos.set(durationNanos);
            data.totalTimeNanos.addAndGet(durationNanos);
            data.executionCount.incrementAndGet();
            data.totalEntities.add(entitiesProcessed);
            
            // Update min atomically
            long currentMin;
            do {
                currentMin = data.minTimeNanos.get();
            } while (durationNanos < currentMin && 
                     !data.minTimeNanos.compareAndSet(currentMin, durationNanos));
            
            // Update max atomically
            long currentMax;
            do {
                currentMax = data.maxTimeNanos.get();
            } while (durationNanos > currentMax && 
                     !data.maxTimeNanos.compareAndSet(currentMax, durationNanos));
        }

        @Override
        public void onError(System system, Throwable error) {
            getOrCreate(system).errorCount.incrementAndGet();
        }

        @Override
        public void recordArchetype(System system, Archetype archetype) {
            getOrCreate(system).totalArchetypes.increment();
        }

        @Override
        public Metrics getMetrics(System system) {
            ProfileData data = profileData.get(system.id);
            if (data == null) return Metrics.EMPTY;
            
            long count = data.executionCount.get();
            long total = data.totalTimeNanos.get();
            double avgMs = count > 0 ? (total / count) / 1_000_000.0 : 0;
            long entities = data.totalEntities.sum();
            double avgEps = count > 0 && total > 0 
                ? (entities / (total / 1_000_000_000.0))
                : 0;
            
            return new Metrics(
                system.name,
                count,
                total,
                data.lastTimeNanos.get(),
                data.minTimeNanos.get() == Long.MAX_VALUE ? 0 : data.minTimeNanos.get(),
                data.maxTimeNanos.get(),
                (int) entities,
                data.totalArchetypes.intValue(),
                avgMs,
                avgEps,
                data.errorCount.get()
            );
        }

        @Override
        public void reset(System system) {
            profileData.remove(system.id);
        }
        
        public void resetAll() {
            profileData.clear();
        }
    }

    // ========================================================================
    // RECORDS
    // ========================================================================

    /**
     * Component access descriptor.
     */
    public record ComponentAccess(
        int componentId,
        Class<?> componentClass,
        AccessMode mode
    ) {}

    /**
     * System descriptor containing all metadata.
     */
    public record Descriptor(
        String name,
        UUID id,
        Class<? extends System> systemClass,
        ComponentMask requiredMask,
        ComponentMask excludedMask,
        ComponentMask optionalMask,
        ComponentMask writeMask,
        int priority,
        ExecutionPhase phase,
        Set<Class<? extends System>> dependencies,
        Set<Class<? extends System>> dependents,
        boolean threadSafe,
        boolean stateless,
        boolean profiled,
        ParallelStrategy parallelStrategy
    ) {
        /**
         * Create descriptor from system using annotation processing.
         */
        public static Descriptor from(System system, SystemAnnotationProcessor processor) {
            return new Descriptor(
                system.name,
                system.id,
                system.getClass(),
                system.requiredMask.clone(),
                system.excludedMask.clone(),
                system.optionalMask.clone(),
                system.writeMask.clone(),
                system.priority,
                system.phase,
                processor != null ? processor.getDependencies(system.getClass()) : Set.of(),
                processor != null ? processor.getDependents(system.getClass()) : Set.of(),
                system.isThreadSafe(),
                system.isStateless(),
                system.isProfiled(),
                system.parallelStrategy
            );
        }
    }

    /**
     * Execution context passed to systems.
     */
    public record ExecutionContext(
        World world,
        float deltaTime,
        long frameNumber,
        Instant frameStart,
        JobSystem jobSystem,
        SystemProfiler profiler,
        Map<String, Object> frameData
    ) {
        @SuppressWarnings("unchecked")
        public <T> T getData(String key, T defaultValue) {
            Object value = frameData.get(key);
            return value != null ? (T) value : defaultValue;
        }

        public void setData(String key, Object value) {
            frameData.put(key, value);
        }

        public Duration elapsed() {
            return Duration.between(frameStart, Instant.now());
        }

        /**
         * Create context with no-op profiler.
         */
        public static ExecutionContext simple(World world, float deltaTime) {
            return new ExecutionContext(
                world, deltaTime, 0, Instant.now(),
                null, SystemProfiler.NOOP, new ConcurrentHashMap<>()
            );
        }
    }

    /**
     * Performance metrics snapshot.
     */
    public record Metrics(
        String systemName,
        long executionCount,
        long totalTimeNanos,
        long lastTimeNanos,
        long minTimeNanos,
        long maxTimeNanos,
        int entitiesProcessed,
        int archetypesProcessed,
        double avgTimeMs,
        double avgEntitiesPerSecond,
        long errorCount
    ) {
        public static final Metrics EMPTY = new Metrics(
            "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
        
        public double getLastTimeMs() {
            return lastTimeNanos / 1_000_000.0;
        }
        
        public double getMinTimeMs() {
            return minTimeNanos / 1_000_000.0;
        }
        
        public double getMaxTimeMs() {
            return maxTimeNanos / 1_000_000.0;
        }
    }

    // ========================================================================
    // ANNOTATION PROCESSOR - Handles annotations at registration time
    // ========================================================================

    /**
     * Processes system annotations at registration time.
     * Separate from System class to avoid runtime reflection overhead.
     */
    public static final class SystemAnnotationProcessor {
        
        private final ComponentRegistry registry;
        private final Map<Class<? extends System>, ProcessedAnnotations> cache = 
            new ConcurrentHashMap<>();

        public record ProcessedAnnotations(
            List<ComponentAccess> required,
            List<ComponentAccess> excluded,
            List<ComponentAccess> optional,
            Set<Class<? extends System>> dependencies,
            Set<Class<? extends System>> dependents,
            int priority,
            ExecutionPhase phase,
            boolean threadSafe,
            boolean stateless,
            boolean profiled,
            ParallelStrategy parallelStrategy
        ) {}

        public SystemAnnotationProcessor(ComponentRegistry registry) {
            this.registry = Objects.requireNonNull(registry);
        }

        /**
         * Process annotations for a system class.
         */
        public ProcessedAnnotations process(Class<? extends System> systemClass) {
            return cache.computeIfAbsent(systemClass, this::processClass);
        }

        private ProcessedAnnotations processClass(Class<? extends System> systemClass) {
            List<ComponentAccess> required = new ArrayList<>();
            List<ComponentAccess> excluded = new ArrayList<>();
            List<ComponentAccess> optional = new ArrayList<>();
            Set<Class<? extends System>> dependencies = new HashSet<>();
            Set<Class<? extends System>> dependents = new HashSet<>();
            
            // Process RequireComponents (including repeated)
            RequireComponents[] requireAnnotations = 
                systemClass.getAnnotationsByType(RequireComponents.class);
            for (RequireComponents ann : requireAnnotations) {
                AccessMode mode = ann.mode();
                for (Class<?> comp : ann.value()) {
                    ComponentRegistry.ComponentType type = registry.getType(comp);
                    required.add(new ComponentAccess(type.id(), comp, mode));
                }
            }
            
            // Process ExcludeComponents
            if (systemClass.isAnnotationPresent(ExcludeComponents.class)) {
                for (Class<?> comp : systemClass.getAnnotation(ExcludeComponents.class).value()) {
                    ComponentRegistry.ComponentType type = registry.getType(comp);
                    excluded.add(new ComponentAccess(type.id(), comp, AccessMode.READ_ONLY));
                }
            }
            
            // Process OptionalComponents
            if (systemClass.isAnnotationPresent(OptionalComponents.class)) {
                for (Class<?> comp : systemClass.getAnnotation(OptionalComponents.class).value()) {
                    ComponentRegistry.ComponentType type = registry.getType(comp);
                    optional.add(new ComponentAccess(type.id(), comp, AccessMode.READ_WRITE));
                }
            }
            
            // Process DependsOn
            if (systemClass.isAnnotationPresent(DependsOn.class)) {
                Collections.addAll(dependencies, 
                    systemClass.getAnnotation(DependsOn.class).value());
            }
            
            // Process RunBefore
            if (systemClass.isAnnotationPresent(RunBefore.class)) {
                Collections.addAll(dependents,
                    systemClass.getAnnotation(RunBefore.class).value());
            }
            
            // Process Priority
            int priority = systemClass.isAnnotationPresent(Priority.class)
                ? systemClass.getAnnotation(Priority.class).value()
                : 0;
            
            // Process Phase
            ExecutionPhase phase = systemClass.isAnnotationPresent(Phase.class)
                ? systemClass.getAnnotation(Phase.class).value()
                : ExecutionPhase.UPDATE;
            
            // Process ThreadSafe
            boolean threadSafe = systemClass.isAnnotationPresent(ThreadSafe.class);
            ParallelStrategy strategy = ParallelStrategy.NONE;
            if (threadSafe) {
                strategy = systemClass.getAnnotation(ThreadSafe.class).strategy();
            }
            
            // Process Stateless
            boolean stateless = systemClass.isAnnotationPresent(Stateless.class);
            
            // Process Profiled
            boolean profiled = systemClass.isAnnotationPresent(Profiled.class);
            
            return new ProcessedAnnotations(
                List.copyOf(required),
                List.copyOf(excluded),
                List.copyOf(optional),
                Set.copyOf(dependencies),
                Set.copyOf(dependents),
                priority,
                phase,
                threadSafe,
                stateless,
                profiled,
                strategy
            );
        }

        /**
         * Apply processed annotations to a system.
         */
        public void applyTo(System system) {
            ProcessedAnnotations ann = process(system.getClass());
            
            // Apply masks
            for (ComponentAccess access : ann.required) {
                system.requiredMask.set(access.componentId);
                if (access.mode == AccessMode.WRITE_ONLY || 
                    access.mode == AccessMode.READ_WRITE) {
                    system.writeMask.set(access.componentId);
                }
            }
            
            for (ComponentAccess access : ann.excluded) {
                system.excludedMask.set(access.componentId);
            }
            
            for (ComponentAccess access : ann.optional) {
                system.optionalMask.set(access.componentId);
            }
            
            // Apply other properties
            system.priority = ann.priority;
            system.phase = ann.phase;
            system.parallelStrategy = ann.parallelStrategy;
            system.dependencies.addAll(ann.dependencies);
            system.dependents.addAll(ann.dependents);
        }

        /**
         * Get dependencies for a system class.
         */
        public Set<Class<? extends System>> getDependencies(Class<? extends System> systemClass) {
            return process(systemClass).dependencies;
        }

        /**
         * Get dependents for a system class.
         */
        public Set<Class<? extends System>> getDependents(Class<? extends System> systemClass) {
            return process(systemClass).dependents;
        }

        /**
         * Clear cache.
         */
        public void clearCache() {
            cache.clear();
        }
    }

    // ========================================================================
    // CORE STATE
    // ========================================================================

    /** System name for debugging and logging */
    public final String name;

    /** System unique identifier */
    public final UUID id;

    /** Priority for execution ordering (lower = earlier) */
    protected volatile int priority;

    /** Execution phase */
    protected volatile ExecutionPhase phase = ExecutionPhase.UPDATE;

    /** Whether system is enabled */
    private volatile boolean enabled = true;

    /** Current system state */
    private volatile State state = State.CREATED;

    /** Parallel execution strategy */
    protected volatile ParallelStrategy parallelStrategy = ParallelStrategy.NONE;

    // Component masks (unlimited capacity)
    protected final ComponentMask requiredMask = new ComponentMask();
    protected final ComponentMask excludedMask = new ComponentMask();
    protected final ComponentMask optionalMask = new ComponentMask();
    protected final ComponentMask writeMask = new ComponentMask();

    // Dependencies (type-safe)
    protected final Set<Class<? extends System>> dependencies = ConcurrentHashMap.newKeySet();
    protected final Set<Class<? extends System>> dependents = ConcurrentHashMap.newKeySet();

    // Error tracking (minimal, profiling is external)
    protected final AtomicReference<Throwable> lastError = new AtomicReference<>();
    protected final AtomicLong errorCount = new AtomicLong();

    // Change detection
    protected final AtomicLong lastProcessedVersion = new AtomicLong(0);

    // Resources managed by this system
    private final List<AutoCloseable> managedResources = new CopyOnWriteArrayList<>();

    // Injected dependencies (no singletons)
    private volatile ComponentRegistry componentRegistry;
    private volatile SystemProfiler profiler = SystemProfiler.NOOP;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    protected System(String name) {
        this.name = Objects.requireNonNull(name, "System name cannot be null");
        this.id = UUID.randomUUID();
        this.priority = 0;
    }

    protected System(String name, int priority) {
        this(name);
        this.priority = priority;
    }

    /**
     * Constructor with dependency injection.
     */
    protected System(String name, ComponentRegistry registry) {
        this(name);
        this.componentRegistry = registry;
    }

    // ========================================================================
    // DEPENDENCY INJECTION
    // ========================================================================

    /**
     * Inject component registry. Called by World/SystemManager.
     */
    public void injectRegistry(ComponentRegistry registry) {
        this.componentRegistry = registry;
    }

    /**
     * Inject profiler. Called by World/SystemManager.
     */
    public void injectProfiler(SystemProfiler profiler) {
        this.profiler = profiler != null ? profiler : SystemProfiler.NOOP;
    }

    /**
     * Get component registry.
     */
    protected ComponentRegistry getRegistry() {
        if (componentRegistry == null) {
            throw new IllegalStateException(
                "ComponentRegistry not injected. System must be registered with a World.");
        }
        return componentRegistry;
    }

    // ========================================================================
    // COMPONENT MASK CONFIGURATION - Fluent API
    // ========================================================================

    /**
     * Require a component type for this system.
     */
    protected final System require(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = getRegistry().getType(componentClass);
        requiredMask.set(type.id());
        return this;
    }

    /**
     * Require a component with specific access mode.
     */
    protected final System require(Class<?> componentClass, AccessMode mode) {
        ComponentRegistry.ComponentType type = getRegistry().getType(componentClass);
        requiredMask.set(type.id());
        if (mode == AccessMode.WRITE_ONLY || mode == AccessMode.READ_WRITE) {
            writeMask.set(type.id());
        }
        return this;
    }

    /**
     * Require multiple component types.
     */
    @SafeVarargs
    protected final System require(Class<?>... componentClasses) {
        for (Class<?> clazz : componentClasses) {
            require(clazz);
        }
        return this;
    }

    /**
     * Exclude entities with a component type.
     */
    protected final System exclude(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = getRegistry().getType(componentClass);
        excludedMask.set(type.id());
        return this;
    }

    /**
     * Exclude multiple component types.
     */
    @SafeVarargs
    protected final System exclude(Class<?>... componentClasses) {
        for (Class<?> clazz : componentClasses) {
            exclude(clazz);
        }
        return this;
    }

    /**
     * Mark component as optional.
     */
    protected final System optional(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = getRegistry().getType(componentClass);
        optionalMask.set(type.id());
        return this;
    }

    /**
     * Mark component as written by this system.
     */
    protected final System writes(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = getRegistry().getType(componentClass);
        writeMask.set(type.id());
        return this;
    }

    /**
     * Add runtime dependency (type-safe).
     */
    protected final System dependsOn(Class<? extends System> systemClass) {
        dependencies.add(systemClass);
        return this;
    }

    /**
     * Add multiple dependencies.
     */
    @SafeVarargs
    protected final System dependsOn(Class<? extends System>... systemClasses) {
        Collections.addAll(dependencies, systemClasses);
        return this;
    }

    /**
     * Declare this system runs before another.
     */
    protected final System runsBefore(Class<? extends System> systemClass) {
        dependents.add(systemClass);
        return this;
    }

    /**
     * Set execution phase.
     */
    protected final System inPhase(ExecutionPhase phase) {
        this.phase = phase;
        return this;
    }

    /**
     * Set parallel strategy.
     */
    protected final System withParallelStrategy(ParallelStrategy strategy) {
        this.parallelStrategy = strategy;
        return this;
    }

    // ========================================================================
    // ARCHETYPE MATCHING
    // ========================================================================

    /**
     * Check if system should process an archetype.
     */
    public boolean matchesArchetype(Archetype archetype) {
        if (archetype == null) return false;
        ComponentMask mask = archetype.getComponentMask();
        return mask.containsAll(requiredMask) && !mask.intersects(excludedMask);
    }

    /**
     * Get all matching archetypes from world.
     */
    public List<Archetype> getMatchingArchetypes(World world) {
        return world.queryArchetypes(requiredMask, excludedMask, optionalMask);
    }

    // ========================================================================
    // LIFECYCLE HOOKS
    // ========================================================================

    /**
     * Initialize system. Called once when registered to world.
     */
    public final void initialize(World world) {
        if (state != State.CREATED && state != State.REGISTERED) {
            throw new IllegalStateException("Cannot initialize system in state: " + state);
        }
        
        state = State.INITIALIZING;
        try {
            onInitialize(world);
            state = State.READY;
        } catch (Throwable t) {
            state = State.ERROR;
            onError(world, t);
            throw t;
        }
    }

    /**
     * Override for custom initialization.
     */
    protected void onInitialize(World world) {
        // Override in subclass
    }

    /**
     * Called before update loop each frame.
     */
    protected void onBeforeUpdate(World world, float deltaTime) {
        // Override in subclass
    }

    /**
     * Called before update with full execution context.
     */
    protected void onBeforeUpdate(ExecutionContext context) {
        onBeforeUpdate(context.world(), context.deltaTime());
    }

    /**
     * Main update method - called for each matching archetype.
     * This is the primary method to override.
     */
    public abstract void update(World world, Archetype archetype, float deltaTime);

    /**
     * Update with full execution context.
     */
    public void update(ExecutionContext context, Archetype archetype) {
        update(context.world(), archetype, context.deltaTime());
    }

    /**
     * Called after update loop each frame.
     */
    protected void onAfterUpdate(World world, float deltaTime) {
        // Override in subclass
    }

    /**
     * Called after update with full execution context.
     */
    protected void onAfterUpdate(ExecutionContext context) {
        onAfterUpdate(context.world(), context.deltaTime());
    }

    /**
     * Cleanup system. Called when world shuts down.
     */
    public final void shutdown(World world) {
        if (state == State.SHUTDOWN || state == State.DISPOSED) return;
        
        state = State.SHUTTING_DOWN;
        try {
            onShutdown(world);
        } finally {
            closeManagedResources();
            state = State.SHUTDOWN;
        }
    }

    /**
     * Override for custom shutdown.
     */
    protected void onShutdown(World world) {
        // Override in subclass
    }

    /**
     * Called when an error occurs during execution.
     */
    protected void onError(World world, Throwable error) {
        lastError.set(error);
        errorCount.incrementAndGet();
        profiler.onError(this, error);
    }

    // ========================================================================
    // EXECUTION - Clean separation, profiling is external
    // ========================================================================

    /**
     * Execute system for all matching archetypes.
     * This is the main entry point called by the scheduler.
     */
    public final ExecutionResult execute(ExecutionContext context) {
        if (!enabled || state == State.PAUSED || state == State.SHUTDOWN) {
            return ExecutionResult.SKIPPED;
        }

        State previousState = state;
        state = State.RUNNING;
        
        long startTime = java.lang.System.nanoTime();
        profiler.onExecutionStart(this);
        
        int entityCount = 0;
        ExecutionResult result = ExecutionResult.SUCCESS;

        try {
            onBeforeUpdate(context);

            List<Archetype> archetypes = getMatchingArchetypes(context.world());
            
            entityCount = switch (parallelStrategy) {
                case NONE -> executeSequential(context, archetypes);
                case ARCHETYPES -> executeParallelArchetypes(context, archetypes);
                case ENTITIES -> executeParallelEntities(context, archetypes);
                case FULL -> executeFullParallel(context, archetypes);
                case CUSTOM -> executeCustom(context, archetypes);
            };

            onAfterUpdate(context);

        } catch (Throwable t) {
            state = State.ERROR;
            onError(context.world(), t);
            result = ExecutionResult.ERROR;
        }

        long duration = java.lang.System.nanoTime() - startTime;
        profiler.onExecutionEnd(this, duration, entityCount);
        
        if (result != ExecutionResult.ERROR) {
            state = State.READY;
        }
        
        return result;
    }

    /**
     * Simple execute without context (for compatibility).
     */
    public final ExecutionResult execute(World world, float deltaTime) {
        return execute(ExecutionContext.simple(world, deltaTime));
    }

    private int executeSequential(ExecutionContext context, List<Archetype> archetypes) {
        int entityCount = 0;
        for (Archetype archetype : archetypes) {
            profiler.recordArchetype(this, archetype);
            update(context, archetype);
            entityCount += archetype.getEntityCount();
        }
        return entityCount;
    }

    private int executeParallelArchetypes(ExecutionContext context, List<Archetype> archetypes) {
        JobSystem jobs = context.jobSystem();
        if (jobs == null) {
            return executeSequential(context, archetypes);
        }

        jobs.parallelForEach(archetypes, archetype -> {
            profiler.recordArchetype(this, archetype);
            update(context, archetype);
        });
        
        return archetypes.stream().mapToInt(Archetype::getEntityCount).sum();
    }

    private int executeParallelEntities(ExecutionContext context, List<Archetype> archetypes) {
        int entityCount = 0;
        for (Archetype archetype : archetypes) {
            profiler.recordArchetype(this, archetype);
            updateParallelEntities(context, archetype);
            entityCount += archetype.getEntityCount();
        }
        return entityCount;
    }

    /**
     * Override for custom parallel entity processing.
     */
    protected void updateParallelEntities(ExecutionContext context, Archetype archetype) {
        update(context, archetype);
    }

    private int executeFullParallel(ExecutionContext context, List<Archetype> archetypes) {
        JobSystem jobs = context.jobSystem();
        if (jobs == null) {
            return executeSequential(context, archetypes);
        }

        jobs.parallelForEach(archetypes, archetype -> {
            profiler.recordArchetype(this, archetype);
            updateParallelEntities(context, archetype);
        });
        
        return archetypes.stream().mapToInt(Archetype::getEntityCount).sum();
    }

    /**
     * Override for fully custom parallel execution.
     */
    protected int executeCustom(ExecutionContext context, List<Archetype> archetypes) {
        return executeSequential(context, archetypes);
    }

    /**
     * Execution result enum.
     */
    public enum ExecutionResult {
        SUCCESS,
        SKIPPED,
        ERROR
    }

    // ========================================================================
    // STATE MANAGEMENT
    // ========================================================================

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { 
        this.enabled = enabled; 
    }

    public System enable() { 
        enabled = true;
        return this;
    }

    public System disable() { 
        enabled = false;
        return this;
    }

    public void pause() {
        if (state == State.READY || state == State.RUNNING) {
            state = State.PAUSED;
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.READY;
        }
    }

    public State getState() { 
        return state; 
    }

    public boolean isRunnable() {
        return enabled && (state == State.READY || state == State.RUNNING);
    }

    public boolean isThreadSafe() {
        return parallelStrategy != ParallelStrategy.NONE || 
               getClass().isAnnotationPresent(ThreadSafe.class);
    }

    public boolean isStateless() {
        return getClass().isAnnotationPresent(Stateless.class);
    }

    public boolean isProfiled() {
        return profiler != SystemProfiler.NOOP || 
               getClass().isAnnotationPresent(Profiled.class);
    }

    // ========================================================================
    // RESOURCE MANAGEMENT
    // ========================================================================

    /**
     * Register a resource to be automatically closed on shutdown.
     */
    protected <T extends AutoCloseable> T manage(T resource) {
        if (resource != null) {
            managedResources.add(resource);
        }
        return resource;
    }

    /**
     * Unregister a managed resource.
     */
    protected void unmanage(AutoCloseable resource) {
        managedResources.remove(resource);
    }

    private void closeManagedResources() {
        for (AutoCloseable resource : managedResources) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log but don't throw
                java.lang.System.err.println("[ECS] Error closing resource in system '" + 
                    name + "': " + e.getMessage());
            }
        }
        managedResources.clear();
    }

    @Override
    public void close() {
        if (state != State.DISPOSED) {
            closeManagedResources();
            state = State.DISPOSED;
        }
    }

    // ========================================================================
    // METRICS (Delegated to profiler)
    // ========================================================================

    /**
     * Get metrics from profiler.
     */
    public Metrics getMetrics() {
        return profiler.getMetrics(this);
    }

    /**
     * Reset profiler metrics.
     */
    public void resetMetrics() {
        profiler.reset(this);
    }

    // ========================================================================
    // DESCRIPTOR & METADATA
    // ========================================================================

    /**
     * Get system descriptor.
     */
    public Descriptor getDescriptor() {
        return Descriptor.from(this, null);
    }

    /**
     * Get system descriptor with annotation processor.
     */
    public Descriptor getDescriptor(SystemAnnotationProcessor processor) {
        return Descriptor.from(this, processor);
    }

    public ComponentMask getRequiredMask() { return requiredMask; }
    public ComponentMask getExcludedMask() { return excludedMask; }
    public ComponentMask getOptionalMask() { return optionalMask; }
    public ComponentMask getWriteMask() { return writeMask; }

    public Set<Class<? extends System>> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public Set<Class<? extends System>> getDependents() {
        return Collections.unmodifiableSet(dependents);
    }

    public int getPriority() { return priority; }
    public ExecutionPhase getPhase() { return phase; }
    public ParallelStrategy getParallelStrategy() { return parallelStrategy; }

    // ========================================================================
    // ERROR HANDLING
    // ========================================================================

    public Optional<Throwable> getLastError() {
        return Optional.ofNullable(lastError.get());
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public void clearErrors() {
        lastError.set(null);
        errorCount.set(0);
        if (state == State.ERROR) {
            state = State.READY;
        }
    }

    // ========================================================================
    // OBJECT METHODS
    // ========================================================================

    @Override
    public String toString() {
        return String.format("System[%s, phase=%s, priority=%d, state=%s, enabled=%s]",
            name, phase, priority, state, enabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof System other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // ========================================================================
    // BUILDER FOR PROGRAMMATIC SYSTEM CREATION
    // ========================================================================

    /**
     * Builder for creating systems programmatically without subclassing.
     */
    public static final class Builder {
        
        private String name;
        private int priority = 0;
        private ExecutionPhase phase = ExecutionPhase.UPDATE;
        private ParallelStrategy parallelStrategy = ParallelStrategy.NONE;
        private final Set<Class<?>> required = new HashSet<>();
        private final Set<Class<?>> excluded = new HashSet<>();
        private final Set<Class<?>> optional = new HashSet<>();
        private final Set<Class<?>> writes = new HashSet<>();
        private final Set<Class<? extends System>> dependencies = new HashSet<>();
        private BiConsumer<World, Archetype> updateFunction;
        private Consumer<World> initFunction;
        private Consumer<World> shutdownFunction;
        private ComponentRegistry registry;

        public Builder(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public Builder require(Class<?>... components) {
            Collections.addAll(required, components);
            return this;
        }

        public Builder exclude(Class<?>... components) {
            Collections.addAll(excluded, components);
            return this;
        }

        public Builder optional(Class<?>... components) {
            Collections.addAll(optional, components);
            return this;
        }

        public Builder writes(Class<?>... components) {
            Collections.addAll(writes, components);
            return this;
        }

        @SafeVarargs
        public final Builder dependsOn(Class<? extends System>... systems) {
            Collections.addAll(dependencies, systems);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder phase(ExecutionPhase phase) {
            this.phase = phase;
            return this;
        }

        public Builder parallel(ParallelStrategy strategy) {
            this.parallelStrategy = strategy;
            return this;
        }

        public Builder onUpdate(BiConsumer<World, Archetype> function) {
            this.updateFunction = function;
            return this;
        }

        public Builder onInit(Consumer<World> function) {
            this.initFunction = function;
            return this;
        }

        public Builder onShutdown(Consumer<World> function) {
            this.shutdownFunction = function;
            return this;
        }

        public Builder registry(ComponentRegistry registry) {
            this.registry = registry;
            return this;
        }

        public System build() {
            if (updateFunction == null) {
                throw new IllegalStateException("Update function is required");
            }
            
            final BiConsumer<World, Archetype> update = updateFunction;
            final Consumer<World> init = initFunction;
            final Consumer<World> shutdown = shutdownFunction;

            System system = new System(name, priority) {
                @Override
                public void update(World world, Archetype archetype, float deltaTime) {
                    update.accept(world, archetype);
                }
                
                @Override
                protected void onInitialize(World world) {
                    if (init != null) init.accept(world);
                }
                
                @Override
                protected void onShutdown(World world) {
                    if (shutdown != null) shutdown.accept(world);
                }
            };

            system.phase = this.phase;
            system.parallelStrategy = this.parallelStrategy;
            system.dependencies.addAll(this.dependencies);

            if (registry != null) {
                system.injectRegistry(registry);
                for (Class<?> c : required) system.require(c);
                for (Class<?> c : excluded) system.exclude(c);
                for (Class<?> c : optional) system.optional(c);
                for (Class<?> c : writes) system.writes(c);
            }

            return system;
        }

        /**
         * Create a new builder.
         */
        public static Builder create(String name) {
            return new Builder(name);
        }
    }
}

    // ========================================================================
    // REFINEMENT LAYER - Production-Ready Hot-Path Optimizations
    // ========================================================================

    /**
     * Performance-critical refinements for production use.
     * This layer provides zero-allocation hot paths and intelligent caching.
     */
    public static final class Refinement {

        private Refinement() {} // Utility class

        // ====================================================================
        // BAKED MASK CONTAINER - Frozen masks for hot-path iteration
        // ====================================================================

        /**
         * Immutable, cache-friendly mask container for hot-path archetype matching.
         * All volatile/lock overhead is eliminated after baking.
         */
        public static final class BakedMasks {
            
            private final long[] required;
            private final long[] excluded;
            private final long[] optional;
            private final long[] writes;
            private final int requiredLength;
            private final int excludedLength;
            private final int hash;

            private BakedMasks(long[] required, long[] excluded, long[] optional, long[] writes) {
                this.required = required;
                this.excluded = excluded;
                this.optional = optional;
                this.writes = writes;
                this.requiredLength = trimmedLength(required);
                this.excludedLength = trimmedLength(excluded);
                this.hash = computeHash();
            }

            private static int trimmedLength(long[] arr) {
                for (int i = arr.length - 1; i >= 0; i--) {
                    if (arr[i] != 0) return i + 1;
                }
                return 0;
            }

            private int computeHash() {
                int result = 17;
                for (int i = 0; i < requiredLength; i++) {
                    result = 31 * result + Long.hashCode(required[i]);
                }
                return result;
            }

            /**
             * Ultra-fast archetype matching - no locks, no allocations.
             * Inlined for JIT optimization.
             */
            public boolean matches(long[] archetypeMask) {
                // Check required components
                int minReq = Math.min(archetypeMask.length, requiredLength);
                for (int i = 0; i < minReq; i++) {
                    if ((archetypeMask[i] & required[i]) != required[i]) {
                        return false;
                    }
                }
                // Check remaining required words exist
                for (int i = minReq; i < requiredLength; i++) {
                    if (required[i] != 0) return false;
                }

                // Check excluded components
                int minExc = Math.min(archetypeMask.length, excludedLength);
                for (int i = 0; i < minExc; i++) {
                    if ((archetypeMask[i] & excluded[i]) != 0) {
                        return false;
                    }
                }

                return true;
            }

            /**
             * Batch match multiple archetypes - cache-friendly sequential access.
             */
            public int matchAll(long[][] archetypeMasks, int[] outIndices) {
                int count = 0;
                for (int i = 0; i < archetypeMasks.length; i++) {
                    if (matches(archetypeMasks[i])) {
                        outIndices[count++] = i;
                    }
                }
                return count;
            }

            /**
             * Create from ComponentMask instances.
             */
            public static BakedMasks from(ComponentMask required, ComponentMask excluded,
                                          ComponentMask optional, ComponentMask writes) {
                return new BakedMasks(
                    required.getWordsSnapshot(),
                    excluded.getWordsSnapshot(),
                    optional != null ? optional.getWordsSnapshot() : new long[0],
                    writes != null ? writes.getWordsSnapshot() : new long[0]
                );
            }

            public long[] getRequired() { return required; }
            public long[] getExcluded() { return excluded; }
            public long[] getOptional() { return optional; }
            public long[] getWrites() { return writes; }
            
            @Override
            public int hashCode() { return hash; }
        }

        // ====================================================================
        // ARCHETYPE CACHE - Intelligent caching with version tracking
        // ====================================================================

        /**
         * Thread-safe archetype cache with structural change detection.
         */
        public static final class ArchetypeCache {

            private volatile CachedResult cachedResult;
            private final System owner;

            private static final class CachedResult {
                final List<Archetype> archetypes;
                final long[] archetypeMasks;
                final int[] entityCounts;
                final long version;
                final int totalEntities;

                CachedResult(List<Archetype> archetypes, long version) {
                    this.archetypes = List.copyOf(archetypes);
                    this.archetypeMasks = new long[archetypes.size()][];
                    this.entityCounts = new int[archetypes.size()];
                    this.version = version;
                    
                    int total = 0;
                    for (int i = 0; i < archetypes.size(); i++) {
                        Archetype a = archetypes.get(i);
                        this.archetypeMasks[i] = a.getComponentMask().getWordsSnapshot();
                        this.entityCounts[i] = a.getEntityCount();
                        total += this.entityCounts[i];
                    }
                    this.totalEntities = total;
                }
            }

            public ArchetypeCache(System owner) {
                this.owner = owner;
            }

            /**
             * Get cached archetypes, rebuilding if version changed.
             */
            public List<Archetype> get(World world, long currentVersion) {
                CachedResult current = cachedResult;
                
                if (current != null && current.version == currentVersion) {
                    return current.archetypes;
                }

                // Rebuild cache
                List<Archetype> fresh = world.queryArchetypes(
                    owner.requiredMask, owner.excludedMask, owner.optionalMask);
                cachedResult = new CachedResult(fresh, currentVersion);
                return cachedResult.archetypes;
            }

            /**
             * Get cached entity count without querying.
             */
            public int getCachedEntityCount() {
                CachedResult current = cachedResult;
                return current != null ? current.totalEntities : 0;
            }

            /**
             * Check if cache is valid.
             */
            public boolean isValid(long currentVersion) {
                CachedResult current = cachedResult;
                return current != null && current.version == currentVersion;
            }

            /**
             * Force invalidation.
             */
            public void invalidate() {
                cachedResult = null;
            }

            /**
             * Get raw archetype masks for batch operations.
             */
            public long[][] getArchetypeMasks() {
                CachedResult current = cachedResult;
                return current != null ? current.archetypeMasks : new long[0][];
            }
        }

        // ====================================================================
        // EXECUTION BUDGET - Frame-time aware execution control
        // ====================================================================

        /**
         * Manages execution budget across systems within a frame.
         */
        public static final class ExecutionBudget {

            private static final long DEFAULT_FRAME_BUDGET_NS = 16_666_667L; // 60 FPS
            private static final long DEFAULT_SYSTEM_BUDGET_NS = 2_000_000L;  // 2ms

            private final long frameBudgetNanos;
            private final long systemBudgetNanos;
            private volatile long frameStartNanos;
            private final AtomicLong consumedNanos = new AtomicLong();
            private final AtomicInteger systemsExecuted = new AtomicInteger();
            private final AtomicInteger systemsSkipped = new AtomicInteger();

            public ExecutionBudget() {
                this(DEFAULT_FRAME_BUDGET_NS, DEFAULT_SYSTEM_BUDGET_NS);
            }

            public ExecutionBudget(long frameBudgetNanos, long systemBudgetNanos) {
                this.frameBudgetNanos = frameBudgetNanos;
                this.systemBudgetNanos = systemBudgetNanos;
            }

            /**
             * Start a new frame.
             */
            public void beginFrame() {
                frameStartNanos = java.lang.System.nanoTime();
                consumedNanos.set(0);
                systemsExecuted.set(0);
                systemsSkipped.set(0);
            }

            /**
             * Check remaining budget.
             */
            public long remainingNanos() {
                return frameBudgetNanos - (java.lang.System.nanoTime() - frameStartNanos);
            }

            /**
             * Check if over frame budget.
             */
            public boolean isOverBudget() {
                return remainingNanos() <= 0;
            }

            /**
             * Check if system can execute within budget.
             */
            public boolean canExecute(System system, double avgExecutionMs) {
                long avgNanos = (long) (avgExecutionMs * 1_000_000);
                return remainingNanos() > avgNanos;
            }

            /**
             * Record system execution.
             */
            public void recordExecution(long durationNanos, boolean executed) {
                if (executed) {
                    consumedNanos.addAndGet(durationNanos);
                    systemsExecuted.incrementAndGet();
                } else {
                    systemsSkipped.incrementAndGet();
                }
            }

            /**
             * Get budget utilization (0.0 - 1.0+).
             */
            public double getUtilization() {
                return (double) consumedNanos.get() / frameBudgetNanos;
            }

            /**
             * Get frame time consumed in milliseconds.
             */
            public double getConsumedMs() {
                return consumedNanos.get() / 1_000_000.0;
            }

            public int getSystemsExecuted() { return systemsExecuted.get(); }
            public int getSystemsSkipped() { return systemsSkipped.get(); }
            public long getFrameBudgetNanos() { return frameBudgetNanos; }
            public long getSystemBudgetNanos() { return systemBudgetNanos; }
        }

        // ====================================================================
        // TICK SCHEDULER - Frequency control for non-critical systems
        // ====================================================================

        /**
         * Controls execution frequency for systems that don't need per-frame updates.
         */
        public static final class TickScheduler {

            private final int interval;
            private final int offset;
            private int counter;
            private long lastExecutionFrame;

            /**
             * Create scheduler with interval.
             * @param interval Run every N ticks
             */
            public TickScheduler(int interval) {
                this(interval, 0);
            }

            /**
             * Create scheduler with interval and offset (for load distribution).
             * @param interval Run every N ticks
             * @param offset Starting offset (spreads systems across frames)
             */
            public TickScheduler(int interval, int offset) {
                this.interval = Math.max(1, interval);
                this.offset = offset % this.interval;
                this.counter = this.offset;
            }

            /**
             * Check if system should run this tick.
             */
            public boolean shouldRun(long frameNumber) {
                if (interval == 1) return true;
                
                counter++;
                if (counter >= interval) {
                    counter = 0;
                    lastExecutionFrame = frameNumber;
                    return true;
                }
                return false;
            }

            /**
             * Force next tick to execute.
             */
            public void forceNext() {
                counter = interval - 1;
            }

            /**
             * Reset counter.
             */
            public void reset() {
                counter = offset;
            }

            /**
             * Get frames since last execution.
             */
            public long framesSinceExecution(long currentFrame) {
                return currentFrame - lastExecutionFrame;
            }

            public int getInterval() { return interval; }
            public int getOffset() { return offset; }
        }

        // ====================================================================
        // BACKPRESSURE-AWARE JOB EXECUTOR
        // ====================================================================

        /**
         * Enhanced job executor with backpressure, saturation detection,
         * and main-thread protection.
         */
        public static final class SafeJobExecutor {

            private final JobSystem jobSystem;
            private final int saturationThreshold;
            private final long mainThreadId;
            private final AtomicLong inlineExecutions = new AtomicLong();
            private final AtomicLong parallelExecutions = new AtomicLong();
            private final AtomicLong rejections = new AtomicLong();

            public SafeJobExecutor(JobSystem jobSystem) {
                this(jobSystem, 500); // Conservative default
            }

            public SafeJobExecutor(JobSystem jobSystem, int saturationThreshold) {
                this.jobSystem = jobSystem;
                this.saturationThreshold = saturationThreshold;
                this.mainThreadId = Thread.currentThread().getId();
            }

            /**
             * Check if executor is saturated.
             */
            public boolean isSaturated() {
                return jobSystem.getQueueSize() >= saturationThreshold;
            }

            /**
             * Check if on main thread.
             */
            public boolean isMainThread() {
                return Thread.currentThread().getId() == mainThreadId;
            }

            /**
             * Execute items in parallel if safe, otherwise sequential.
             */
            public <T> void executeParallel(List<T> items, Consumer<T> action) {
                if (items.isEmpty()) return;

                int size = items.size();
                int threads = jobSystem.getThreadCount();

                // Decide execution strategy
                boolean shouldParallelize = 
                    size > threads * 2 &&           // Enough work to justify overhead
                    !isSaturated() &&               // System not overloaded
                    !isMainThread();                // Not risking main thread

                if (shouldParallelize) {
                    parallelExecutions.incrementAndGet();
                    jobSystem.parallelForEach(items, action);
                } else {
                    inlineExecutions.incrementAndGet();
                    for (T item : items) {
                        action.accept(item);
                    }
                }
            }

            /**
             * Execute indexed range in parallel if safe.
             */
            public void executeParallel(int start, int end, IntConsumer action) {
                int range = end - start;
                if (range <= 0) return;

                int threads = jobSystem.getThreadCount();

                boolean shouldParallelize = 
                    range > threads * 4 &&
                    !isSaturated() &&
                    !isMainThread();

                if (shouldParallelize) {
                    parallelExecutions.incrementAndGet();
                    jobSystem.parallelFor(start, end, action);
                } else {
                    inlineExecutions.incrementAndGet();
                    for (int i = start; i < end; i++) {
                        action.accept(i);
                    }
                }
            }

            /**
             * Submit with fallback to inline execution.
             */
            public boolean submitSafe(Runnable job) {
                if (isSaturated() || isMainThread()) {
                    inlineExecutions.incrementAndGet();
                    job.run();
                    return false;
                }
                
                try {
                    jobSystem.submit(job);
                    parallelExecutions.incrementAndGet();
                    return true;
                } catch (RejectedExecutionException e) {
                    rejections.incrementAndGet();
                    job.run();
                    return false;
                }
            }

            /**
             * Get execution statistics.
             */
            public ExecutorStats getStats() {
                return new ExecutorStats(
                    inlineExecutions.get(),
                    parallelExecutions.get(),
                    rejections.get(),
                    jobSystem.getJobsSubmitted(),
                    jobSystem.getJobsCompleted(),
                    jobSystem.getQueueSize(),
                    jobSystem.getActiveCount()
                );
            }

            public record ExecutorStats(
                long inlineExecutions,
                long parallelExecutions,
                long rejections,
                long jobsSubmitted,
                long jobsCompleted,
                int queueSize,
                int activeThreads
            ) {
                public double parallelRatio() {
                    long total = inlineExecutions + parallelExecutions;
                    return total > 0 ? (double) parallelExecutions / total : 0;
                }
            }
        }

        // ====================================================================
        // BATCH PROCESSOR - Efficient bulk operations
        // ====================================================================

        /**
         * Batches operations for cache-friendly bulk processing.
         * Reduces per-item overhead for homogeneous operations.
         */
        public static final class BatchProcessor<T> {

            private final int batchSize;
            private final Consumer<List<T>> batchConsumer;
            private final List<T> buffer;
            private long itemsProcessed;
            private long batchesProcessed;

            public BatchProcessor(int batchSize, Consumer<List<T>> batchConsumer) {
                this.batchSize = batchSize;
                this.batchConsumer = batchConsumer;
                this.buffer = new ArrayList<>(batchSize);
            }

            /**
             * Add item to batch.
             */
            public void add(T item) {
                buffer.add(item);
                if (buffer.size() >= batchSize) {
                    flush();
                }
            }

            /**
             * Add multiple items.
             */
            public void addAll(Collection<T> items) {
                for (T item : items) {
                    add(item);
                }
            }

            /**
             * Process remaining items.
             */
            public void flush() {
                if (!buffer.isEmpty()) {
                    batchConsumer.accept(buffer);
                    itemsProcessed += buffer.size();
                    batchesProcessed++;
                    buffer.clear();
                }
            }

            /**
             * Process all items with automatic flush.
             */
            public void processAll(Collection<T> items) {
                addAll(items);
                flush();
            }

            /**
             * Reset statistics.
             */
            public void resetStats() {
                itemsProcessed = 0;
                batchesProcessed = 0;
            }

            public long getItemsProcessed() { return itemsProcessed; }
            public long getBatchesProcessed() { return batchesProcessed; }
            public int getBatchSize() { return batchSize; }
            public int getBufferedCount() { return buffer.size(); }
        }

        // ====================================================================
        // PREFETCH HELPER - Cache-warming for predictable access patterns
        // ====================================================================

        /**
         * Provides prefetch hints for predictable memory access.
         * Uses soft prefetch through array access patterns.
         */
        public static final class PrefetchHelper {

            private static final int DEFAULT_DISTANCE = 4;
            private static final int CACHE_LINE_ENTITIES = 8; // Typical cache line

            /**
             * Prefetch-friendly iteration over entity data.
             * Access pattern optimized for CPU cache prefetcher.
             */
            public static void iterateWithPrefetch(int count, IntConsumer processor) {
                // Process in cache-line-sized chunks
                int chunks = count / CACHE_LINE_ENTITIES;
                int remainder = count % CACHE_LINE_ENTITIES;

                for (int chunk = 0; chunk < chunks; chunk++) {
                    int base = chunk * CACHE_LINE_ENTITIES;
                    
                    // Process chunk (prefetcher will kick in)
                    for (int i = 0; i < CACHE_LINE_ENTITIES; i++) {
                        processor.accept(base + i);
                    }
                }

                // Process remainder
                int base = chunks * CACHE_LINE_ENTITIES;
                for (int i = 0; i < remainder; i++) {
                    processor.accept(base + i);
                }
            }

            /**
             * Strided iteration for interleaved component access.
             */
            public static void iterateStrided(int count, int stride, IntConsumer processor) {
                for (int offset = 0; offset < stride; offset++) {
                    for (int i = offset; i < count; i += stride) {
                        processor.accept(i);
                    }
                }
            }

            /**
             * Blocked iteration for 2D-style data.
             */
            public static void iterateBlocked(int width, int height, int blockSize,
                                              BiIntConsumer processor) {
                for (int by = 0; by < height; by += blockSize) {
                    int bh = Math.min(blockSize, height - by);
                    for (int bx = 0; bx < width; bx += blockSize) {
                        int bw = Math.min(blockSize, width - bx);
                        
                        // Process block
                        for (int y = by; y < by + bh; y++) {
                            for (int x = bx; x < bx + bw; x++) {
                                processor.accept(x, y);
                            }
                        }
                    }
                }
            }

            @FunctionalInterface
            public interface BiIntConsumer {
                void accept(int a, int b);
            }
        }
    }

    // ========================================================================
    // REFINED SYSTEM STATE - Integrated with refinement components
    // ========================================================================

    // Baked masks (populated during initialization)
    private volatile Refinement.BakedMasks bakedMasks;
    
    // Archetype cache
    private final Refinement.ArchetypeCache archetypeCache = new Refinement.ArchetypeCache(this);
    
    // Tick scheduler (null = every tick)
    private volatile Refinement.TickScheduler tickScheduler;
    
    // Execution budget (injected by scheduler)
    private volatile Refinement.ExecutionBudget executionBudget;
    
    // Safe executor (injected by world)
    private volatile Refinement.SafeJobExecutor safeExecutor;
    
    // World structure version (for cache invalidation)
    private volatile long lastKnownWorldVersion = -1;

    // ========================================================================
    // REFINED INITIALIZATION
    // ========================================================================

    /**
     * Enhanced initialization that bakes masks and sets up caches.
     */
    private void performRefinedInitialization(World world) {
        // Bake masks for hot-path matching
        bakedMasks = Refinement.BakedMasks.from(
            requiredMask, excludedMask, optionalMask, writeMask);
        
        // Initialize safe executor if job system available
        JobSystem jobs = world.getJobSystem();
        if (jobs != null) {
            safeExecutor = new Refinement.SafeJobExecutor(jobs);
        }
        
        // Warm the archetype cache
        lastKnownWorldVersion = world.getStructureVersion();
        archetypeCache.get(world, lastKnownWorldVersion);
    }

    // ========================================================================
    // REFINED ARCHETYPE MATCHING - Zero-allocation hot path
    // ========================================================================

    /**
     * Optimized archetype matching using baked masks.
     * This is the hot-path version - no locks, no allocations.
     */
    public boolean matchesArchetypeRefined(long[] archetypeMaskWords) {
        if (bakedMasks == null) {
            // Fallback to standard matching if not baked yet
            return matchesArchetype(null); // Will fail gracefully
        }
        return bakedMasks.matches(archetypeMaskWords);
    }

    /**
     * Get matching archetypes with caching.
     * Automatically invalidates when world structure changes.
     */
    public List<Archetype> getMatchingArchetypesRefined(World world) {
        long currentVersion = world.getStructureVersion();
        
        // Check if world structure changed
        if (currentVersion != lastKnownWorldVersion) {
            lastKnownWorldVersion = currentVersion;
            // Cache will auto-rebuild on get()
        }
        
        return archetypeCache.get(world, currentVersion);
    }

    // ========================================================================
    // REFINED EXECUTION - Budget-aware, frequency-controlled
    // ========================================================================

    /**
     * Fully refined execution with all optimizations.
     */
    public final ExecutionResult executeRefined(ExecutionContext context) {
        // Check if enabled and in valid state
        if (!enabled || state == State.PAUSED || state == State.SHUTDOWN) {
            return ExecutionResult.SKIPPED;
        }

        // Check tick scheduler
        if (tickScheduler != null && !tickScheduler.shouldRun(context.frameNumber())) {
            return ExecutionResult.SKIPPED;
        }

        // Check execution budget
        if (executionBudget != null) {
            Metrics metrics = getMetrics();
            if (!executionBudget.canExecute(this, metrics.avgTimeMs())) {
                executionBudget.recordExecution(0, false);
                return ExecutionResult.SKIPPED;
            }
        }

        // Ensure masks are baked
        if (bakedMasks == null) {
            performRefinedInitialization(context.world());
        }

        State previousState = state;
        state = State.RUNNING;
        
        long startTime = java.lang.System.nanoTime();
        profiler.onExecutionStart(this);
        
        int entityCount = 0;
        ExecutionResult result = ExecutionResult.SUCCESS;

        try {
            onBeforeUpdate(context);

            // Use cached archetypes
            List<Archetype> archetypes = getMatchingArchetypesRefined(context.world());
            
            // Execute with appropriate strategy
            entityCount = executeRefinedStrategy(context, archetypes);

            onAfterUpdate(context);

        } catch (Throwable t) {
            state = State.ERROR;
            onError(context.world(), t);
            result = ExecutionResult.ERROR;
        }

        long duration = java.lang.System.nanoTime() - startTime;
        profiler.onExecutionEnd(this, duration, entityCount);
        
        if (executionBudget != null) {
            executionBudget.recordExecution(duration, true);
        }
        
        if (result != ExecutionResult.ERROR) {
            state = State.READY;
        }
        
        return result;
    }

    /**
     * Execute with refined parallel strategy using safe executor.
     */
    private int executeRefinedStrategy(ExecutionContext context, List<Archetype> archetypes) {
        if (archetypes.isEmpty()) return 0;

        // Use safe executor if available
        Refinement.SafeJobExecutor executor = this.safeExecutor;
        
        return switch (parallelStrategy) {
            case NONE -> executeSequentialRefined(context, archetypes);
            
            case ARCHETYPES -> {
                if (executor != null) {
                    executor.executeParallel(archetypes, a -> {
                        profiler.recordArchetype(this, a);
                        update(context, a);
                    });
                    yield archetypes.stream().mapToInt(Archetype::getEntityCount).sum();
                }
                yield executeSequentialRefined(context, archetypes);
            }
            
            case ENTITIES -> {
                int count = 0;
                for (Archetype archetype : archetypes) {
                    profiler.recordArchetype(this, archetype);
                    updateParallelEntitiesRefined(context, archetype, executor);
                    count += archetype.getEntityCount();
                }
                yield count;
            }
            
            case FULL -> {
                if (executor != null) {
                    executor.executeParallel(archetypes, a -> {
                        profiler.recordArchetype(this, a);
                        updateParallelEntitiesRefined(context, a, executor);
                    });
                    yield archetypes.stream().mapToInt(Archetype::getEntityCount).sum();
                }
                yield executeSequentialRefined(context, archetypes);
            }
            
            case CUSTOM -> executeCustom(context, archetypes);
        };
    }

    private int executeSequentialRefined(ExecutionContext context, List<Archetype> archetypes) {
        int count = 0;
        for (Archetype archetype : archetypes) {
            profiler.recordArchetype(this, archetype);
            update(context, archetype);
            count += archetype.getEntityCount();
        }
        return count;
    }

    /**
     * Override for refined parallel entity processing.
     */
    protected void updateParallelEntitiesRefined(ExecutionContext context, 
                                                  Archetype archetype,
                                                  Refinement.SafeJobExecutor executor) {
        // Default: use standard update
        update(context, archetype);
    }

    // ========================================================================
    // REFINED CONFIGURATION - Fluent API extensions
    // ========================================================================

    /**
     * Configure tick interval (run every N frames).
     */
    protected final System everyNTicks(int interval) {
        this.tickScheduler = new Refinement.TickScheduler(interval);
        return this;
    }

    /**
     * Configure tick interval with offset for load distribution.
     */
    protected final System everyNTicks(int interval, int offset) {
        this.tickScheduler = new Refinement.TickScheduler(interval, offset);
        return this;
    }

    /**
     * Inject execution budget (called by scheduler).
     */
    public void injectExecutionBudget(Refinement.ExecutionBudget budget) {
        this.executionBudget = budget;
    }

    /**
     * Inject safe executor (called by world/scheduler).
     */
    public void injectSafeExecutor(Refinement.SafeJobExecutor executor) {
        this.safeExecutor = executor;
    }

    /**
     * Force cache invalidation.
     */
    public void invalidateCache() {
        archetypeCache.invalidate();
        lastKnownWorldVersion = -1;
    }

    /**
     * Get baked masks (for external optimization).
     */
    public Refinement.BakedMasks getBakedMasks() {
        return bakedMasks;
    }

    /**
     * Get archetype cache statistics.
     */
    public int getCachedEntityCount() {
        return archetypeCache.getCachedEntityCount();
    }

    // ========================================================================
    // HELPER: Create prefetch-optimized entity processor
    // ========================================================================

    /**
     * Create a batch processor for this system.
     */
    protected <T> Refinement.BatchProcessor<T> createBatchProcessor(
            int batchSize, Consumer<List<T>> processor) {
        return new Refinement.BatchProcessor<>(batchSize, processor);
    }

    /**
     * Iterate entities with cache-friendly prefetch pattern.
     */
    protected void iterateEntitiesOptimized(int count, IntConsumer processor) {
        Refinement.PrefetchHelper.iterateWithPrefetch(count, processor);
    }

    // ========================================================================
    // WORLD INTERFACE ADDITIONS (Add these to your World class)
    // ========================================================================

    /*
     * Add to World class:
     * 
     * private final AtomicLong structureVersion = new AtomicLong();
     * private JobSystem jobSystem;
     * 
     * public long getStructureVersion() {
     *     return structureVersion.get();
     * }
     * 
     * public void incrementStructureVersion() {
     *     structureVersion.incrementAndGet();
     * }
     * 
     * public JobSystem getJobSystem() {
     *     return jobSystem;
     * }
     * 
     * // Call incrementStructureVersion() whenever:
     * // - An archetype is created
     * // - An archetype is destroyed  
     * // - An entity moves between archetypes
     */

    // ========================================================================
    // COMPONENT MASK ADDITIONS
    // ========================================================================

    // Add this method to ComponentMask class if not already present:
    /*
     * public long[] getWordsSnapshot() {
     *     long stamp = lock.readLock();
     *     try {
     *         return words.clone();
     *     } finally {
     *         lock.unlockRead(stamp);
     *     }
     * }
     */
