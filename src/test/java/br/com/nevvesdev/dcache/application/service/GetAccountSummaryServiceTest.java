package br.com.nevvesdev.dcache.application.service;

import br.com.nevvesdev.dcache.application.port.in.AccountSummary;
import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.AccountNotFoundException;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAccountSummaryServiceTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-14T12:00:00Z");

    private final FakeAccountRepository repository = new FakeAccountRepository();
    private final FakeAccountCache cache = new FakeAccountCache();
    private final GetAccountSummaryService service = new GetAccountSummaryService(repository, cache);

    @Test
    void shouldReturnSummaryFromCacheWithoutQueryingRepositoryOnCacheHit() {
        Account account = anAccount();
        cache.preload(account);

        AccountSummary summary = service.execute(account.id());

        assertThat(summary).isEqualTo(AccountSummary.from(account));
        assertThat(repository.findByIdCalls()).isZero();
        assertThat(cache.putCalls()).isZero();
    }

    @Test
    void shouldLoadFromRepositoryAndPopulateCacheOnCacheMiss() {
        Account account = anAccount();
        repository.store(account);

        AccountSummary summary = service.execute(account.id());

        assertThat(summary).isEqualTo(AccountSummary.from(account));
        assertThat(repository.findByIdCalls()).isEqualTo(1);
        assertThat(cache.putCalls()).isEqualTo(1);
        assertThat(cache.find(account.id())).contains(account);
    }

    @Test
    void shouldQueryRepositoryOnlyOnceForConsecutiveReads() {
        Account account = anAccount();
        repository.store(account);

        AccountSummary first = service.execute(account.id());
        AccountSummary second = service.execute(account.id());

        assertThat(second).isEqualTo(first);
        assertThat(repository.findByIdCalls()).isEqualTo(1);
        assertThat(cache.putCalls()).isEqualTo(1);
    }

    @Test
    void shouldFailAndKeepCacheUntouchedWhenAccountDoesNotExist() {
        AccountId unknown = AccountId.generate();

        assertThatThrownBy(() -> service.execute(unknown))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining(unknown.toString());

        assertThat(cache.putCalls()).isZero();
    }

    @Test
    void shouldRejectNullAccountId() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("accountId must not be null");
    }

    @Test
    void shouldRejectNullDependencies() {
        assertThatThrownBy(() -> new GetAccountSummaryService(null, cache))
            .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new GetAccountSummaryService(repository, null))
            .isInstanceOf(InvalidValueException.class);
    }

    private static Account anAccount() {
        return new Account(AccountId.generate(), Balance.of("150.00"), CreditLimit.of("1000.00"), UPDATED_AT);
    }

    private static final class FakeAccountRepository implements AccountRepository {

        private final Map<AccountId, Account> accounts = new HashMap<>();
        private int findByIdCalls;

        void store(Account account) {
            accounts.put(account.id(), account);
        }

        int findByIdCalls() {
            return findByIdCalls;
        }

        @Override
        public Optional<Account> findById(AccountId id) {
            findByIdCalls++;
            return Optional.ofNullable(accounts.get(id));
        }
    }

    private static final class FakeAccountCache implements AccountCache {

        private final Map<AccountId, Account> entries = new HashMap<>();
        private int putCalls;

        void preload(Account account) {
            entries.put(account.id(), account);
        }

        int putCalls() {
            return putCalls;
        }

        @Override
        public Optional<Account> find(AccountId id) {
            return Optional.ofNullable(entries.get(id));
        }

        @Override
        public void put(Account account) {
            putCalls++;
            entries.put(account.id(), account);
        }

        @Override
        public void evict(AccountId id) {
            entries.remove(id);
        }
    }
}
