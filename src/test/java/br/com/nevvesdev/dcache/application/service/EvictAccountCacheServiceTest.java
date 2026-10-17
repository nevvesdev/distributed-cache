package br.com.nevvesdev.dcache.application.service;

import br.com.nevvesdev.dcache.application.exception.CacheUnavailableException;
import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvictAccountCacheServiceTest {

    private final RecordingCache cache = new RecordingCache();
    private final EvictAccountCacheService service = new EvictAccountCacheService(cache);

    @Test
    void shouldEvictAccountFromCache() {
        AccountId id = AccountId.generate();

        service.execute(id);

        assertThat(cache.evicted()).containsExactly(id);
    }

    @Test
    void shouldPropagateCacheFailure() {
        cache.failWith(new CacheUnavailableException("cache down", new IllegalStateException("redis")));

        assertThatThrownBy(() -> service.execute(AccountId.generate()))
            .isInstanceOf(CacheUnavailableException.class);
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("accountId must not be null");
    }

    @Test
    void shouldRejectNullCache() {
        assertThatThrownBy(() -> new EvictAccountCacheService(null))
            .isInstanceOf(InvalidValueException.class);
    }

    private static final class RecordingCache implements AccountCache {

        private final List<AccountId> evicted = new ArrayList<>();
        private RuntimeException failure;

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        List<AccountId> evicted() {
            return evicted;
        }

        @Override
        public Optional<Account> find(AccountId id) {
            return Optional.empty();
        }

        @Override
        public void put(Account account) {
        }

        @Override
        public void evict(AccountId id) {
            if (failure != null) {
                throw failure;
            }
            evicted.add(id);
        }
    }
}
