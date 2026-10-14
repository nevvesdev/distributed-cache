package br.com.nevvesdev.dcache.application.port.in;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import br.com.nevvesdev.dcache.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountSummaryTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-14T12:00:00Z");

    @Test
    void shouldBuildSummaryFromAccount() {
        Account account = new Account(
            AccountId.of("3f2b8c1e-9a4d-4e7b-8c55-1d2e3f4a5b6c"),
            Balance.of("-200.00"),
            CreditLimit.of("500.00"),
            UPDATED_AT);

        AccountSummary summary = AccountSummary.from(account);

        assertThat(summary.accountId()).isEqualTo(account.id());
        assertThat(summary.balance()).isEqualTo(Money.of("-200.00"));
        assertThat(summary.creditLimit()).isEqualTo(Money.of("500.00"));
        assertThat(summary.usedLimit()).isEqualTo(Money.of("200.00"));
        assertThat(summary.availableLimit()).isEqualTo(Money.of("300.00"));
        assertThat(summary.availableFunds()).isEqualTo(Money.of("300.00"));
        assertThat(summary.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void shouldRejectNullAccount() {
        assertThatThrownBy(() -> AccountSummary.from(null))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("account must not be null");
    }
}
