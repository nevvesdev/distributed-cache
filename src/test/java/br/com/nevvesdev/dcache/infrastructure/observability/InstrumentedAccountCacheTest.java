package br.com.nevvesdev.dcache.infrastructure.observability;

import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstrumentedAccountCacheTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ScriptedCache delegate = new ScriptedCache();
    private final InstrumentedAccountCache cache = new InstrumentedAccountCache(delegate, registry);

    private static Account anAccount() {
        return new Account(AccountId.generate(), Balance.of("10.00"), CreditLimit.of("20.00"), Instant.parse("2026-10-17T12:00:00Z"));
    }

    private double lookups(String result) {
        return registry.counter("dcache.cache.lookups", "result", result).count();
    }

    private long operations(String operation, String outcome) {
        return registry.timer("dcache.cache.operations", "operation", operation, "outcome", outcome).count();
    }

    @Test
    void shouldRegisterLookupCountersUpFront() {
        assertThat(registry.find("dcache.cache.lookups").counters()).hasSize(2);
        assertThat(lookups("hit")).isZero();
        assertThat(lookups("miss")).isZero();
    }

    @Test
    void shouldCountHit() {
        Account account = anAccount();
        delegate.store(account);

        assertThat(cache.find(account.id())).contains(account);

        assertThat(lookups("hit")).isEqualTo(1.0);
        assertThat(lookups("miss")).isZero();
        assertThat(operations("find", "success")).isEqualTo(1);
    }

    @Test
    void shouldCountMiss() {
        assertThat(cache.find(AccountId.generate())).isEmpty();

        assertThat(lookups("miss")).isEqualTo(1.0);
        assertThat(lookups("hit")).isZero();
        assertThat(operations("find", "success")).isEqualTo(1);
    }

    @Test
    void shouldRecordPutAndEvictOperations() {
        Account account = anAccount();

        cache.put(account);
        cache.evict(account.id());

        assertThat(operations("put", "success")).isEqualTo(1);
        assertThat(operations("evict", "success")).isEqualTo(1);
    }

    @Test
    void shouldRecordFailuresAndRethrowThem() {
        delegate.fail();
        AccountId id = AccountId.generate();

        assertThatThrownBy(() -> cache.find(id)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cache.put(anAccount())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cache.evict(id)).isInstanceOf(IllegalStateException.class);

        assertThat(operations("find", "error")).isEqualTo(1);
        assertThat(operations("put", "error")).isEqualTo(1);
        assertThat(operations("evict", "error")).isEqualTo(1);
        assertThat(lookups("hit")).isZero();
        assertThat(lookups("miss")).isZero();
    }

    @Test
    void shouldRejectNullArgumentsWithoutRecordingOperations() {
        assertThatThrownBy(() -> cache.find(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> cache.put(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> cache.evict(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new InstrumentedAccountCache(null, registry)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new InstrumentedAccountCache(delegate, null)).isInstanceOf(InvalidValueException.class);

        assertThat(operations("find", "error")).isZero();
        assertThat(operations("find", "success")).isZero();
    }

    private static final class ScriptedCache implements AccountCache {

        private final Map<AccountId, Account> entries = new HashMap<>();
        private boolean failing;

        void fail() {
            failing = true;
        }

        void store(Account account) {
            entries.put(account.id(), account);
        }

        @Override
        public Optional<Account> find(AccountId id) {
            failIfNeeded();
            return Optional.ofNullable(entries.get(id));
        }

        @Override
        public void put(Account account) {
            failIfNeeded();
            entries.put(account.id(), account);
        }

        @Override
        public void evict(AccountId id) {
            failIfNeeded();
            entries.remove(id);
        }

        private void failIfNeeded() {
            if (failing) {
                throw new IllegalStateException("redis unavailable");
            }
        }
    }
}
