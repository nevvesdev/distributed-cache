package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.domain.shared.Preconditions;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class CacheCircuitBreaker {

    private final int failureThreshold;
    private final long openMillis;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openUntil = new AtomicLong();

    CacheCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be at least 1");
        }
        if (openDuration == null || openDuration.isZero() || openDuration.isNegative()) {
            throw new IllegalArgumentException("openDuration must be a positive duration");
        }
        this.failureThreshold = failureThreshold;
        this.openMillis = openDuration.toMillis();
        this.clock = Preconditions.requireNonNull(clock, "clock");
    }

    boolean allowRequest() {
        long deadline = openUntil.get();
        if (deadline == 0) {
            return true;
        }
        long now = clock.millis();
        if (now < deadline) {
            return false;
        }
        return openUntil.compareAndSet(deadline, now + openMillis);
    }

    void recordSuccess() {
        consecutiveFailures.set(0);
        openUntil.set(0);
    }

    void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openUntil.set(clock.millis() + openMillis);
        }
    }

    boolean isOpen() {
        return openUntil.get() != 0;
    }
}
