package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

public final class TtlPolicy {

    private final Duration base;
    private final double jitter;
    private final DoubleSupplier random;

    TtlPolicy(Duration base, double jitter, DoubleSupplier random) {
        Preconditions.requireNonNull(base, "base");
        Preconditions.requireNonNull(random, "random");
        if (base.isZero() || base.isNegative()) {
            throw new InvalidValueException("base ttl must be a positive duration");
        }
        if (!(jitter >= 0 && jitter <= 1)) {
            throw new InvalidValueException("jitter must be between 0 and 1");
        }
        this.base = base;
        this.jitter = jitter;
        this.random = random;
    }

    public static TtlPolicy fixed(Duration base) {
        return new TtlPolicy(base, 0, () -> 0);
    }

    public static TtlPolicy withJitter(Duration base, double jitter) {
        return new TtlPolicy(base, jitter, () -> ThreadLocalRandom.current().nextDouble());
    }

    Duration next() {
        long extraMillis = Math.round(base.toMillis() * jitter * random.getAsDouble());
        return base.plusMillis(extraMillis);
    }
}
