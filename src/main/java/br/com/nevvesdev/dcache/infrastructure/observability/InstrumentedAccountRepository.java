package br.com.nevvesdev.dcache.infrastructure.observability;

import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Optional;

public class InstrumentedAccountRepository implements AccountRepository {

    private static final String LOOKUPS = "dcache.repository.lookups";

    private final AccountRepository delegate;
    private final MeterRegistry registry;

    public InstrumentedAccountRepository(AccountRepository delegate, MeterRegistry registry) {
        this.delegate = Preconditions.requireNonNull(delegate, "delegate");
        this.registry = Preconditions.requireNonNull(registry, "registry");
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        Preconditions.requireNonNull(id, "id");
        Timer.Sample sample = Timer.start(registry);
        String outcome = "error";
        try {
            Optional<Account> result = delegate.findById(id);
            outcome = result.isPresent() ? "found" : "not_found";
            return result;
        } finally {
            sample.stop(Timer.builder(LOOKUPS)
                .description("Source of truth lookups by outcome")
                .tag("outcome", outcome)
                .register(registry));
        }
    }
}
