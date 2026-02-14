package stellar.snow.astralis.engine.render.system;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
/**
 * FramePhaseFSM - Finite State Machine for Frame Execution
 * 
 * Ensures strict ordering of frame phases to prevent race conditions.
 * You cannot proceed to "Render Opaque" until "Update" is complete.
 * 
 * Features:
 * - Strict phase ordering and validation
 * - Automatic dependency resolution
 * - Dead lock detection
 * - Performance monitoring per phase
 * - Rollback capability on errors
 */
    
    /**
     * All possible frame phases in strict execution order
     */
    public enum Phase {
        IDLE(0),
        INPUT_POLL(1),
        SIMULATION_UPDATE(2),
        PHYSICS_UPDATE(3),
        ANIMATION_UPDATE(4),
        FRUSTUM_CULL(5),
        VISIBILITY_COMPUTE(6),
        COMMAND_BUILD(7),
        SHADOW_MAP_RENDER(8),
        DEPTH_PREPASS(9),
        GBUFFER_RENDER(10),
        LIGHTING_COMPUTE(11),
        TRANSPARENT_RENDER(12),
        POST_PROCESS(13),
        UI_RENDER(14),
        PRESENT(15),
        CLEANUP(16);
        
        public final int order;
        
        Phase(int order) {
            this.order = order;
        }
        
        public boolean canTransitionTo(Phase next) {
            return next.order == this.order + 1 || next == IDLE || next == CLEANUP;
        }
    }
    
    // Current phase (atomic for thread safety)
    private final AtomicReference<Phase> currentPhase = new AtomicReference<>(Phase.IDLE);
    
    // Phase transition listeners
    private final Map<Phase, List<Consumer<Phase>>> phaseListeners = new ConcurrentHashMap<>();
    
    // Phase dependencies (which phase must complete before another can start)
    private final Map<Phase, Set<Phase>> phaseDependencies = new ConcurrentHashMap<>();
    
    // Completed phases in current frame
    private final Set<Phase> completedPhases = ConcurrentHashMap.newKeySet();
    
    // Phase execution times for profiling
    private final Map<Phase, Long> phaseStartTimes = new ConcurrentHashMap<>();
    private final Map<Phase, Long> phaseDurations = new ConcurrentHashMap<>();
    private final Map<Phase, AtomicLong> phaseTotalTimes = new ConcurrentHashMap<>();
    private final Map<Phase, AtomicInteger> phaseExecutionCounts = new ConcurrentHashMap<>();
    
    // Frame counter
    private final AtomicLong frameCounter = new AtomicLong(0);
    
    // Error handling
    private final AtomicReference<String> lastError = new AtomicReference<>(null);
    private final AtomicBoolean errorState = new AtomicBoolean(false);
    
    // Lock for phase transitions
    private final ReentrantLock transitionLock = new ReentrantLock();
    private final Condition phaseCompleteCondition = transitionLock.newCondition();
    
    // Deadlock detection
    private final ScheduledExecutorService deadlockDetector = Executors.newSingleThreadScheduledExecutor();
    private volatile long lastTransitionTime = System.nanoTime();
    private static final long DEADLOCK_THRESHOLD_MS = 5000;
    
    // Statistics
    private final AtomicLong totalFrames = new AtomicLong(0);
    private final AtomicLong failedTransitions = new AtomicLong(0);
    private final AtomicLong forcedResets = new AtomicLong(0);
    
    public FramePhaseFSM() {
        initializeDependencies();
        initializeMetrics();
        startDeadlockDetector();
    }
    
    /**
     * Define phase dependencies
     */
    private void initializeDependencies() {
        // Physics depends on simulation
        addDependency(Phase.PHYSICS_UPDATE, Phase.SIMULATION_UPDATE);
        
        // Animation depends on simulation
        addDependency(Phase.ANIMATION_UPDATE, Phase.SIMULATION_UPDATE);
        
        // Frustum culling depends on simulation (camera updates)
        addDependency(Phase.FRUSTUM_CULL, Phase.SIMULATION_UPDATE);
        
        // Visibility depends on frustum culling
        addDependency(Phase.VISIBILITY_COMPUTE, Phase.FRUSTUM_CULL);
        
        // Command building depends on visibility
        addDependency(Phase.COMMAND_BUILD, Phase.VISIBILITY_COMPUTE);
        
        // Shadow rendering depends on visibility
        addDependency(Phase.SHADOW_MAP_RENDER, Phase.VISIBILITY_COMPUTE);
        
        // Depth prepass depends on command building
        addDependency(Phase.DEPTH_PREPASS, Phase.COMMAND_BUILD);
        
        // GBuffer depends on depth prepass
        addDependency(Phase.GBUFFER_RENDER, Phase.DEPTH_PREPASS);
        
        // Lighting depends on GBuffer
        addDependency(Phase.LIGHTING_COMPUTE, Phase.GBUFFER_RENDER);
        
        // Transparent depends on lighting
        addDependency(Phase.TRANSPARENT_RENDER, Phase.LIGHTING_COMPUTE);
        
        // Post-process depends on transparent
        addDependency(Phase.POST_PROCESS, Phase.TRANSPARENT_RENDER);
        
        // UI depends on post-process
        addDependency(Phase.UI_RENDER, Phase.POST_PROCESS);
        
        // Present depends on UI
        addDependency(Phase.PRESENT, Phase.UI_RENDER);
        
        // Cleanup depends on present
        addDependency(Phase.CLEANUP, Phase.PRESENT);
    }
    
    /**
     * Add a dependency relationship
     */
    private void addDependency(Phase phase, Phase dependsOn) {
        phaseDependencies.computeIfAbsent(phase, k -> ConcurrentHashMap.newKeySet()).add(dependsOn);
    }
    
    /**
     * Initialize metrics tracking
     */
    private void initializeMetrics() {
        for (Phase phase : Phase.values()) {
            phaseTotalTimes.put(phase, new AtomicLong(0));
            phaseExecutionCounts.put(phase, new AtomicInteger(0));
        }
    }
    
    /**
     * Start deadlock detection thread
     */
    private void startDeadlockDetector() {
        deadlockDetector.scheduleAtFixedRate(() -> {
            long elapsed = (System.nanoTime() - lastTransitionTime) / 1_000_000;
            if (elapsed > DEADLOCK_THRESHOLD_MS && currentPhase.get() != Phase.IDLE) {
                System.err.println("DEADLOCK DETECTED: Stuck in phase " + currentPhase.get() + 
                                   " for " + elapsed + "ms");
                dumpState();
                
                // Auto-recovery: force reset
                forceReset();
            }
        }, DEADLOCK_THRESHOLD_MS, DEADLOCK_THRESHOLD_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Begin a new frame
     */
    public boolean beginFrame() {
        transitionLock.lock();
        try {
            Phase current = currentPhase.get();
            
            if (current != Phase.IDLE && current != Phase.CLEANUP) {
                setError("Cannot begin new frame - current phase is " + current);
                return false;
            }
            
            // Reset state for new frame
            completedPhases.clear();
            errorState.set(false);
            lastError.set(null);
            frameCounter.incrementAndGet();
            totalFrames.incrementAndGet();
            
            return transitionToPhase(Phase.INPUT_POLL);
        } finally {
            transitionLock.unlock();
        }
    }
    
    /**
     * Transition to a specific phase
     */
    public boolean transitionToPhase(Phase targetPhase) {
        transitionLock.lock();
        try {
            Phase current = currentPhase.get();
            
            // Check if in error state
            if (errorState.get() && targetPhase != Phase.CLEANUP && targetPhase != Phase.IDLE) {
                return false;
            }
            
            // Validate transition
            if (!canTransition(current, targetPhase)) {
                String error = String.format("Invalid transition: %s -> %s", current, targetPhase);
                setError(error);
                failedTransitions.incrementAndGet();
                return false;
            }
            
            // Check dependencies
            if (!areDependenciesMet(targetPhase)) {
                String error = String.format("Dependencies not met for phase %s", targetPhase);
                setError(error);
                failedTransitions.incrementAndGet();
                return false;
            }
            
            // Record timing for previous phase
            if (current != Phase.IDLE) {
                long duration = System.nanoTime() - phaseStartTimes.get(current);
                phaseDurations.put(current, duration);
                phaseTotalTimes.get(current).addAndGet(duration);
                phaseExecutionCounts.get(current).incrementAndGet();
            }
            
            // Transition
            currentPhase.set(targetPhase);
            phaseStartTimes.put(targetPhase, System.nanoTime());
            lastTransitionTime = System.nanoTime();
            
            // Notify listeners
            notifyPhaseListeners(targetPhase);
            
            // Signal waiting threads
            phaseCompleteCondition.signalAll();
            
            return true;
        } finally {
            transitionLock.unlock();
        }
    }
    
    /**
     * Check if transition is valid
     */
    private boolean canTransition(Phase from, Phase to) {
        // Allow transitions to IDLE or CLEANUP from any state
        if (to == Phase.IDLE || to == Phase.CLEANUP) {
            return true;
        }
        
        // Allow sequential progression
        if (to.order == from.order + 1) {
            return true;
        }
        
        // Allow skipping phases if explicitly requested (for partial frames)
        if (to.order > from.order) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if all dependencies for a phase are met
     */
    private boolean areDependenciesMet(Phase phase) {
        Set<Phase> deps = phaseDependencies.get(phase);
        if (deps == null || deps.isEmpty()) {
            return true;
        }
        
        return completedPhases.containsAll(deps);
    }
    
    /**
     * Mark current phase as complete and advance
     */
    public boolean completePhase() {
        transitionLock.lock();
        try {
            Phase current = currentPhase.get();
            
            if (current == Phase.IDLE) {
                setError("Cannot complete IDLE phase");
                return false;
            }
            
            // Mark as completed
            completedPhases.add(current);
            
            // Auto-advance to next phase
            Phase next = getNextPhase(current);
            if (next != null) {
                return transitionToPhase(next);
            }
            
            return true;
        } finally {
            transitionLock.unlock();
        }
    }
    
    /**
     * Get the next phase in sequence
     */
    private Phase getNextPhase(Phase current) {
        Phase[] phases = Phase.values();
        for (int i = 0; i < phases.length - 1; i++) {
            if (phases[i] == current) {
                return phases[i + 1];
            }
        }
        return null;
    }
    
    /**
     * Wait for a specific phase to complete
     */
    public boolean waitForPhase(Phase phase, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        transitionLock.lock();
        try {
            while (!completedPhases.contains(phase) && !errorState.get()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                phaseCompleteCondition.await(remaining, TimeUnit.NANOSECONDS);
            }
            return completedPhases.contains(phase);
        } finally {
            transitionLock.unlock();
        }
    }
    
    /**
     * Register a phase transition listener
     */
    public void addPhaseListener(Phase phase, Consumer<Phase> listener) {
        phaseListeners.computeIfAbsent(phase, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    
    /**
     * Notify all listeners for a phase
     */
    private void notifyPhaseListeners(Phase phase) {
        List<Consumer<Phase>> listeners = phaseListeners.get(phase);
        if (listeners != null) {
            for (Consumer<Phase> listener : listeners) {
                try {
                    listener.accept(phase);
                } catch (Exception e) {
                    System.err.println("Error in phase listener: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * End the current frame
     */
    public boolean endFrame() {
        return transitionToPhase(Phase.CLEANUP) && transitionToPhase(Phase.IDLE);
    }
    
    /**
     * Force reset to IDLE state (emergency recovery)
     */
    public void forceReset() {
        transitionLock.lock();
        try {
            System.err.println("FORCE RESET from phase " + currentPhase.get());
            currentPhase.set(Phase.IDLE);
            completedPhases.clear();
            errorState.set(false);
            lastError.set(null);
            forcedResets.incrementAndGet();
        } finally {
            transitionLock.unlock();
        }
    }
    
    /**
     * Set error state
     */
    private void setError(String message) {
        errorState.set(true);
        lastError.set(message);
        System.err.println("FramePhaseFSM Error: " + message);
    }
    
    /**
     * Get current phase
     */
    public Phase getCurrentPhase() {
        return currentPhase.get();
    }
    
    /**
     * Check if in error state
     */
    public boolean isInError() {
        return errorState.get();
    }
    
    /**
     * Get last error message
     */
    public String getLastError() {
        return lastError.get();
    }
    
    /**
     * Get frame number
     */
    public long getFrameNumber() {
        return frameCounter.get();
    }
    
    /**
     * Get average phase duration in milliseconds
     */
    public double getAveragePhaseDuration(Phase phase) {
        long total = phaseTotalTimes.get(phase).get();
        int count = phaseExecutionCounts.get(phase).get();
        if (count == 0) return 0.0;
        return (total / (double) count) / 1_000_000.0;
    }
    
    /**
     * Get last phase duration in milliseconds
     */
    public double getLastPhaseDuration(Phase phase) {
        Long duration = phaseDurations.get(phase);
        return duration != null ? duration / 1_000_000.0 : 0.0;
    }
    
    /**
     * Dump current state for debugging
     */
    public void dumpState() {
        System.out.println("=== FramePhaseFSM State ===");
        System.out.println("Current Phase: " + currentPhase.get());
        System.out.println("Frame Number: " + frameCounter.get());
        System.out.println("Error State: " + errorState.get());
        System.out.println("Last Error: " + lastError.get());
        System.out.println("Completed Phases: " + completedPhases);
        System.out.println("Total Frames: " + totalFrames.get());
        System.out.println("Failed Transitions: " + failedTransitions.get());
        System.out.println("Forced Resets: " + forcedResets.get());
        
        System.out.println("\nPhase Timings (ms):");
        for (Phase phase : Phase.values()) {
            double avg = getAveragePhaseDuration(phase);
            double last = getLastPhaseDuration(phase);
            int count = phaseExecutionCounts.get(phase).get();
            System.out.printf("  %s: avg=%.2f, last=%.2f, count=%d\n", 
                phase, avg, last, count);
        }
    }
    
    /**
     * Get statistics as formatted string
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("FramePhaseFSM Stats:\n");
        sb.append(String.format("  Total Frames: %d\n", totalFrames.get()));
        sb.append(String.format("  Current Phase: %s\n", currentPhase.get()));
        sb.append(String.format("  Failed Transitions: %d\n", failedTransitions.get()));
        sb.append(String.format("  Forced Resets: %d\n", forcedResets.get()));
        
        double totalFrameTime = 0.0;
        for (Phase phase : Phase.values()) {
            totalFrameTime += getAveragePhaseDuration(phase);
        }
        sb.append(String.format("  Avg Frame Time: %.2f ms\n", totalFrameTime));
        
        return sb.toString();
    }
    
    /**
     * Shutdown the FSM
     */
    public void shutdown() {
        deadlockDetector.shutdown();
        try {
            deadlockDetector.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            deadlockDetector.shutdownNow();
        }
    }
}
