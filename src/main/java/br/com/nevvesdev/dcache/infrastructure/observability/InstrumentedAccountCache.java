package br.com.nevvesdev.dcache.infrastructure.observability;

import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Optional;
import java.util.function.Supplier;

public class InstrumentedAccountCache implements AccountCache {

    private static final String LOOKUPS = "dcache.cache.lookups";
    private static final String OPERATIONS = "dcache.cache.operations";

    private final AccountCache delegate;
    private final MeterRegistry registry;
    private final Counter hits;
    private final Counter misses;

    public InstrumentedAccountCache(AccountCache delegate, MeterRegistry registry) {
        this.delegate = Preconditions.requireNonNull(delegate, "delegate");
        this.registry = Preconditions.requireNonNull(registry, "registry");
        this.hits = lookupCounter("hit");
        this.misses = lookupCounter("miss");
    }

    @Override
    public Optional<Account> find(AccountId id) {
        Preconditions.requireNonNull(id, "id");
        Optional<Account> result = timed("find", () -> delegate.find(id));
        if (result.isPresent()) {
            hits.increment();
        } else {
            misses.increment();
        }
        return result;
    }

    @Override
    public void put(Account account) {
        Preconditions.requireNonNull(account, "account");
        timed("put", () -> {
            delegate.put(account);
            return null;
        });
    }

    @Override
    public void evict(AccountId id) {
        Preconditions.requireNonNull(id, "id");
        timed("evict", () -> {
            delegate.evict(id);
            return null;
        });
    }

    private Counter lookupCounter(String result) {
        return Counter.builder(LOOKUPS)
            .description("Cache lookups by result")
            .tag("result", result)
            .register(registry);
    }

    private <T> T timed(String operation, Supplier<T> action) {
        Timer.Sample sample = Timer.start(registry);
        String outcome = "error";
        try {
            T result = action.get();
            outcome = "success";
            return result;
        } finally {
            sample.stop(Timer.builder(OPERATIONS)
                .description("Cache operations by outcome")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(registry));
        }
    }
}
