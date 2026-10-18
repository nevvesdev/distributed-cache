package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import br.com.nevvesdev.dcache.application.exception.CacheUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailSafeAccountCacheTest {

    private static final Duration OPEN_DURATION = Duration.ofSeconds(30);

    private final MutableClock clock = new MutableClock();
    private final ScriptedCache delegate = new ScriptedCache();
    private final FailSafeAccountCache cache =
        new FailSafeAccountCache(delegate, new CacheCircuitBreaker(2, OPEN_DURATION, clock));

    private static Account anAccount() {
        return new Account(AccountId.generate(), Balance.of("10.00"), CreditLimit.of("20.00"), Instant.parse("2026-10-17T12:00:00Z"));
    }

    @Test
    void shouldDelegateWhileHealthy() {
        Account account = anAccount();

        cache.put(account);

        assertThat(cache.find(account.id())).contains(account);
        assertThat(delegate.putCalls()).isEqualTo(1);
        assertThat(delegate.findCalls()).isEqualTo(1);
    }

    @Test
    void shouldTreatLookupFailureAsMiss() {
        delegate.fail();

        assertThat(cache.find(AccountId.generate())).isEmpty();
    }

    @Test
    void shouldSwallowWriteFailure() {
        delegate.fail();

        assertThatCode(() -> cache.put(anAccount())).doesNotThrowAnyException();
    }

    @Test
    void shouldStopCallingDelegateWhileCircuitIsOpen() {
        delegate.fail();
        AccountId id = AccountId.generate();

        for (int i = 0; i < 5; i++) {
            cache.find(id);
        }

        assertThat(delegate.findCalls()).isEqualTo(2);
    }

    @Test
    void shouldResumeCallingDelegateAfterOpenDuration() {
        Account account = anAccount();
        delegate.store(account);
        delegate.fail();
        cache.find(account.id());
        cache.find(account.id());
        delegate.recover();

        assertThat(cache.find(account.id())).isEmpty();

        clock.advance(OPEN_DURATION);

        assertThat(cache.find(account.id())).contains(account);
        assertThat(cache.find(account.id())).contains(account);
    }

    @Test
    void shouldRejectNullArguments() {
        assertThatThrownBy(() -> cache.find(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> cache.put(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new FailSafeAccountCache(null, 2, OPEN_DURATION)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldDelegateEviction() {
        Account account = anAccount();
        cache.put(account);

        cache.evict(account.id());

        assertThat(cache.find(account.id())).isEmpty();
        assertThat(delegate.evictCalls()).isEqualTo(1);
    }

    @Test
    void shouldReportEvictionFailureInsteadOfHidingIt() {
        delegate.fail();

        assertThatThrownBy(() -> cache.evict(AccountId.generate()))
            .isInstanceOf(CacheUnavailableException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAttemptEvictionEvenWhileCircuitIsOpen() {
        AccountId id = AccountId.generate();
        delegate.fail();
        cache.find(id);
        cache.find(id);
        delegate.recover();

        cache.evict(id);

        assertThat(delegate.evictCalls()).isEqualTo(1);
        assertThat(cache.find(id)).isEmpty();
        assertThat(delegate.findCalls()).isEqualTo(3);
    }

    @Test
    void shouldRejectNullIdOnEviction() {
        assertThatThrownBy(() -> cache.evict(null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldExposeCircuitState() {
        assertThat(cache.isCircuitOpen()).isFalse();

        delegate.fail();
        cache.find(AccountId.generate());
        cache.find(AccountId.generate());

        assertThat(cache.isCircuitOpen()).isTrue();
    }

    private static final class ScriptedCache implements AccountCache {

        private final Map<AccountId, Account> entries = new HashMap<>();
        private boolean failing;
        private int findCalls;
        private int putCalls;

        void fail() {
            failing = true;
        }

        void recover() {
            failing = false;
        }

        void store(Account account) {
            entries.put(account.id(), account);
        }

        int findCalls() {
            return findCalls;
        }

        int putCalls() {
            return putCalls;
        }

        private int evictCalls;

        int evictCalls() {
            return evictCalls;
        }

        @Override
        public void evict(AccountId id) {
            evictCalls++;
            if (failing) {
                throw new IllegalStateException("redis unavailable");
            }
            entries.remove(id);
        }

        @Override
        public Optional<Account> find(AccountId id) {
            findCalls++;
            if (failing) {
                throw new IllegalStateException("redis unavailable");
            }
            return Optional.ofNullable(entries.get(id));
        }

        @Override
        public void put(Account account) {
            putCalls++;
            if (failing) {
                throw new IllegalStateException("redis unavailable");
            }
            entries.put(account.id(), account);
        }
    }
}
