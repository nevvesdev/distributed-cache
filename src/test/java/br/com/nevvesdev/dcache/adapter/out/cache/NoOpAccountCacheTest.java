package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpAccountCacheTest {

    private final NoOpAccountCache cache = new NoOpAccountCache();

    @Test
    void shouldNeverReturnStoredEntries() {
        Account account = new Account(
            AccountId.generate(), Balance.of("10.00"), CreditLimit.of("20.00"), Instant.parse("2026-10-16T12:00:00Z"));

        cache.put(account);

        assertThat(cache.find(account.id())).isEmpty();
    }

    @Test
    void shouldIgnoreEviction() {
        assertThatCode(() -> cache.evict(AccountId.generate())).doesNotThrowAnyException();
    }
}
