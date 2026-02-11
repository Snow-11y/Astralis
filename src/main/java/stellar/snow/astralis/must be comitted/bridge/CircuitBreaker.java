package stellar.snow.astralis.bridge;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CircuitBreaker - Thread-safe circuit breaker for fault tolerance.
 *
 * <h2>States:</h2>
 * <ul>
 *   <li>CLOSED: Normal operation, requests allowed</li>
 *   <li>OPEN: Failures exceeded threshold, requests blocked</li>
 *   <li>HALF_OPEN: Testing recovery, limited requests allowed</li>
 * </ul>
 */
public final class CircuitBreaker {

    private static final int STATE_CLOSED = 0;
    private static final int STATE_OPEN = 1;
    private static final int STATE_HALF_OPEN = 2;

    private static final VarHandle STATE_HANDLE;
    private static final VarHandle FAILURE_COUNT_HANDLE;
    private static final VarHandle SUCCESS_COUNT_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            STATE_HANDLE = lookup.findVarHandle(CircuitBreaker.class, "state", int.class);
            FAILURE_COUNT_HANDLE = lookup.findVarHandle(CircuitBreaker.class, "failureCount", int.class);
            SUCCESS_COUNT_HANDLE = lookup.findVarHandle(CircuitBreaker.class, "successCount", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final String name;
    private final int failureThreshold;
    private final long resetTimeoutMillis;
    private final int halfOpenSuccessThreshold;

    @SuppressWarnings("FieldMayBeFinal")
    private volatile int state = STATE_CLOSED;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int failureCount = 0;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int successCount = 0;

    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);
    private final AtomicLong totalSuccesses = new AtomicLong(0);
    private final AtomicLong tripCount = new AtomicLong(0);

    public CircuitBreaker(int failureThreshold, long resetTimeoutMillis, String name) {
        this(failureThreshold, resetTimeoutMillis, 3, name);
    }

    public CircuitBreaker(int failureThreshold, long resetTimeoutMillis, int halfOpenSuccessThreshold, String name) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMillis = resetTimeoutMillis;
        this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
        this.name = name;
    }

    /**
     * Checks if a request should be allowed.
     *
     * @return true if the request can proceed
     */
    public boolean allowRequest() {
        int currentState = (int) STATE_HANDLE.get(this);

        return switch (currentState) {
            case STATE_CLOSED -> true;
            case STATE_OPEN -> {
                long now = java.lang.System.currentTimeMillis();
                if (now - lastFailureTime.get() > resetTimeoutMillis) {
                    // Attempt transition to half-open
                    if (STATE_HANDLE.compareAndSet(this, STATE_OPEN, STATE_HALF_OPEN)) {
                        SUCCESS_COUNT_HANDLE.set(this, 0);
                    }
                    yield true;
                }
                yield false;
            }
            case STATE_HALF_OPEN -> true;
            default -> false;
        };
    }

    /**
     * Records a successful operation.
     */
    public void recordSuccess() {
        totalSuccesses.incrementAndGet();
        FAILURE_COUNT_HANDLE.set(this, 0);

        int currentState = (int) STATE_HANDLE.get(this);
        if (currentState == STATE_HALF_OPEN) {
            int newCount = (int) SUCCESS_COUNT_HANDLE.getAndAdd(this, 1) + 1;
            if (newCount >= halfOpenSuccessThreshold) {
                STATE_HANDLE.set(this, STATE_CLOSED);
            }
        }
    }

    /**
     * Records a failed operation.
     */
    public void recordFailure() {
        totalFailures.incrementAndGet();
        lastFailureTime.set(java.lang.System.currentTimeMillis());
        SUCCESS_COUNT_HANDLE.set(this, 0);

        int currentState = (int) STATE_HANDLE.get(this);

        if (currentState == STATE_HALF_OPEN) {
            // Immediate trip back to open
            STATE_HANDLE.set(this, STATE_OPEN);
            tripCount.incrementAndGet();
        } else if (currentState == STATE_CLOSED) {
            int newCount = (int) FAILURE_COUNT_HANDLE.getAndAdd(this, 1) + 1;
            if (newCount >= failureThreshold) {
                STATE_HANDLE.set(this, STATE_OPEN);
                tripCount.incrementAndGet();
            }
        }
    }

    /**
     * @return true if the circuit is open (blocking requests)
     */
    public boolean isOpen() {
        return (int) STATE_HANDLE.get(this) == STATE_OPEN;
    }

    /**
     * @return true if the circuit is closed (normal operation)
     */
    public boolean isClosed() {
        return (int) STATE_HANDLE.get(this) == STATE_CLOSED;
    }

    /**
     * Manually resets the circuit to closed state.
     */
    public void reset() {
        STATE_HANDLE.set(this, STATE_CLOSED);
        FAILURE_COUNT_HANDLE.set(this, 0);
        SUCCESS_COUNT_HANDLE.set(this, 0);
    }

    /**
     * @return current state as string
     */
    public String getStateString() {
        int s = (int) STATE_HANDLE.get(this);
        return switch (s) {
            case STATE_CLOSED -> "CLOSED";
            case STATE_OPEN -> "OPEN";
            case STATE_HALF_OPEN -> "HALF_OPEN";
            default -> "UNKNOWN";
        };
    }

    public String getName() {
        return name;
    }

    public long getTotalFailures() {
        return totalFailures.get();
    }

    public long getTotalSuccesses() {
        return totalSuccesses.get();
    }

    public long getTripCount() {
        return tripCount.get();
    }
}
