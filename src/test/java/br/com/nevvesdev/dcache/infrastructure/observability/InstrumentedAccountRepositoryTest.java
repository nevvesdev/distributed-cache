package br.com.nevvesdev.dcache.infrastructure.observability;

import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstrumentedAccountRepositoryTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private static Account anAccount() {
        return new Account(AccountId.generate(), Balance.of("10.00"), CreditLimit.of("20.00"), Instant.parse("2026-10-17T12:00:00Z"));
    }

    private long lookups(String outcome) {
        return registry.timer("dcache.repository.lookups", "outcome", outcome).count();
    }

    @Test
    void shouldRecordFoundLookup() {
        Account account = anAccount();
        AccountRepository repository = new InstrumentedAccountRepository(id -> Optional.of(account), registry);

        assertThat(repository.findById(account.id())).contains(account);

        assertThat(lookups("found")).isEqualTo(1);
        assertThat(lookups("not_found")).isZero();
        assertThat(lookups("error")).isZero();
    }

    @Test
    void shouldRecordNotFoundLookup() {
        AccountRepository repository = new InstrumentedAccountRepository(id -> Optional.empty(), registry);

        assertThat(repository.findById(AccountId.generate())).isEmpty();

        assertThat(lookups("not_found")).isEqualTo(1);
        assertThat(lookups("found")).isZero();
    }

    @Test
    void shouldRecordFailureAndRethrowIt() {
        AccountRepository repository = new InstrumentedAccountRepository(id -> {
            throw new IllegalStateException("database unavailable");
        }, registry);

        assertThatThrownBy(() -> repository.findById(AccountId.generate()))
            .isInstanceOf(IllegalStateException.class);

        assertThat(lookups("error")).isEqualTo(1);
        assertThat(lookups("found")).isZero();
    }

    @Test
    void shouldRejectNullArguments() {
        AccountRepository repository = new InstrumentedAccountRepository(id -> Optional.empty(), registry);

        assertThatThrownBy(() -> repository.findById(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new InstrumentedAccountRepository(null, registry)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new InstrumentedAccountRepository(id -> Optional.empty(), null)).isInstanceOf(InvalidValueException.class);
    }
}
