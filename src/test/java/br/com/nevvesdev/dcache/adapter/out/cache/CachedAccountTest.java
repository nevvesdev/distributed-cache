package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachedAccountTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-16T09:00:00Z");

    private static Account anAccount() {
        return new Account(AccountId.generate(), Balance.of("-350.75"), CreditLimit.of("1000.00"), UPDATED_AT);
    }

    @Test
    void shouldRoundTripAccount() {
        Account account = anAccount();

        assertThat(CachedAccount.from(account).toDomain()).isEqualTo(account);
    }

    @Test
    void shouldExposeValuesAsPlainText() {
        Account account = anAccount();

        CachedAccount cached = CachedAccount.from(account);

        assertThat(cached.id()).isEqualTo(account.id().toString());
        assertThat(cached.balance()).isEqualTo("-350.75");
        assertThat(cached.creditLimit()).isEqualTo("1000.00");
        assertThat(cached.updatedAt()).isEqualTo("2026-10-16T09:00:00Z");
    }

    @Test
    void shouldRejectInvalidStoredValues() {
        CachedAccount cached = new CachedAccount("not-a-uuid", "1.00", "1.00", "2026-10-16T09:00:00Z");

        assertThatThrownBy(cached::toDomain).isInstanceOf(InvalidValueException.class);
    }
}
