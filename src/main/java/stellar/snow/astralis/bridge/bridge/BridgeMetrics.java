package stellar.snow.astralis.nexus.controlpanel;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * BridgeMetrics - Lock-free performance metrics collection.
 *
 * Uses LongAdder for high-contention counters and VarHandle for single-writer metrics.
 */
public final class BridgeMetrics {

    // ========================================================================
    // HIGH CONTENTION COUNTERS (LongAdder for scalability)
    // ========================================================================

    private final LongAdder registrationCount = new LongAdder();
    private final LongAdder unregistrationCount = new LongAdder();
    private final LongAdder registrationFailures = new LongAdder();
    private final LongAdder tickCount = new LongAdder();
    private final LongAdder tickFailures = new LongAdder();
    private final LongAdder circuitBreakerTrips = new LongAdder();

    // ========================================================================
    // SINGLE-WRITER METRICS (VarHandle for volatile writes)
    // ========================================================================

    private static final VarHandle LAST_TICK_DURATION;
    private static final VarHandle AVG_TICK_DURATION;
    private static final VarHandle MAX_TICK_DURATION;
    private static final VarHandle MIN_TICK_DURATION;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            LAST_TICK_DURATION = lookup.findVarHandle(BridgeMetrics.class, "lastTickDurationNanos", long.class);
            AVG_TICK_DURATION = lookup.findVarHandle(BridgeMetrics.class, "avgTickDurationNanos", long.class);
            MAX_TICK_DURATION = lookup.findVarHandle(BridgeMetrics.class, "maxTickDurationNanos", long.class);
            MIN_TICK_DURATION = lookup.findVarHandle(BridgeMetrics.class, "minTickDurationNanos", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile long lastTickDurationNanos = 0;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long avgTickDurationNanos = 0;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long maxTickDurationNanos = 0;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long minTickDurationNanos = Long.MAX_VALUE;

    // Exponential moving average alpha (1/16 for smooth averaging)
    private static final int EMA_SHIFT = 4;

    // ========================================================================
    // RECORDING METHODS
    // ========================================================================

    public void recordRegistration() {
        registrationCount.increment();
    }

    public void recordUnregistration() {
        unregistrationCount.increment();
    }

    public void recordRegistrationFailure() {
        registrationFailures.increment();
    }

    public void recordTick(long durationNanos) {
        tickCount.increment();
        LAST_TICK_DURATION.setVolatile(this, durationNanos);

        // Update EMA: avg = avg + (new - avg) / 16
        long oldAvg = (long) AVG_TICK_DURATION.get(this);
        long newAvg = oldAvg + ((durationNanos - oldAvg) >> EMA_SHIFT);
        AVG_TICK_DURATION.setVolatile(this, newAvg);

        // Update max (relaxed ordering is fine)
        long currentMax = (long) MAX_TICK_DURATION.get(this);
        if (durationNanos > currentMax) {
            MAX_TICK_DURATION.setVolatile(this, durationNanos);
        }

        // Update min
        long currentMin = (long) MIN_TICK_DURATION.get(this);
        if (durationNanos < currentMin) {
            MIN_TICK_DURATION.setVolatile(this, durationNanos);
        }
    }

    public void recordTickFailure() {
        tickFailures.increment();
    }

    public void recordCircuitBreakerTrip() {
        circuitBreakerTrips.increment();
    }

    // ========================================================================
    // ACCESSORS
    // ========================================================================

    public long getRegistrationCount() {
        return registrationCount.sum();
    }

    public long getUnregistrationCount() {
        return unregistrationCount.sum();
    }

    public long getRegistrationFailures() {
        return registrationFailures.sum();
    }

    public long getTickCount() {
        return tickCount.sum();
    }

    public long getTickFailures() {
        return tickFailures.sum();
    }

    public long getCircuitBreakerTrips() {
        return circuitBreakerTrips.sum();
    }

    public long getLastTickDurationNanos() {
        return (long) LAST_TICK_DURATION.get(this);
    }

    public long getAvgTickDurationNanos() {
        return (long) AVG_TICK_DURATION.get(this);
    }

    public long getMaxTickDurationNanos() {
        return (long) MAX_TICK_DURATION.get(this);
    }

    public long getMinTickDurationNanos() {
        long min = (long) MIN_TICK_DURATION.get(this);
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public double getLastTickDurationMs() {
        return getLastTickDurationNanos() / 1_000_000.0;
    }

    public double getAvgTickDurationMs() {
        return getAvgTickDurationNanos() / 1_000_000.0;
    }

    // ========================================================================
    // SNAPSHOT
    // ========================================================================

    /**
     * Creates an immutable snapshot of current metrics.
     */
    public Snapshot snapshot() {
        return new Snapshot(
                getRegistrationCount(),
                getUnregistrationCount(),
                getRegistrationFailures(),
                getTickCount(),
                getTickFailures(),
                getCircuitBreakerTrips(),
                getLastTickDurationNanos(),
                getAvgTickDurationNanos(),
                getMaxTickDurationNanos(),
                getMinTickDurationNanos()
        );
    }

    public record Snapshot(
            long registrations,
            long unregistrations,
            long registrationFailures,
            long ticks,
            long tickFailures,
            long circuitBreakerTrips,
            long lastTickNanos,
            long avgTickNanos,
            long maxTickNanos,
            long minTickNanos
    ) {
        public double avgTickMs() {
            return avgTickNanos / 1_000_000.0;
        }

        public double maxTickMs() {
            return maxTickNanos / 1_000_000.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "BridgeMetrics[entities=%d, ticks=%d, avgTick=%.2fms, maxTick=%.2fms, failures=%d]",
                    registrations - unregistrations, ticks, avgTickMs(), maxTickMs(), tickFailures
            );
        }
    }

    /**
     * Resets all metrics (for testing or diagnostics).
     */
    public void reset() {
        registrationCount.reset();
        unregistrationCount.reset();
        registrationFailures.reset();
        tickCount.reset();
        tickFailures.reset();
        circuitBreakerTrips.reset();
        LAST_TICK_DURATION.setVolatile(this, 0L);
        AVG_TICK_DURATION.setVolatile(this, 0L);
        MAX_TICK_DURATION.setVolatile(this, 0L);
        MIN_TICK_DURATION.setVolatile(this, Long.MAX_VALUE);
    }
}
