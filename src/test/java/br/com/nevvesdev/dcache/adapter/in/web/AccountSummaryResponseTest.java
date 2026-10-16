package br.com.nevvesdev.dcache.adapter.in.web;

import br.com.nevvesdev.dcache.application.port.in.AccountSummary;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountSummaryResponseTest {

    private static final String ACCOUNT_ID = "3f2b8c1e-9a4d-4e7b-8c55-1d2e3f4a5b6c";
    private static final Instant UPDATED_AT = Instant.parse("2026-10-14T12:00:00Z");

    @Test
    void shouldMapSummaryToResponse() {
        AccountSummary summary = new AccountSummary(
            AccountId.of(ACCOUNT_ID),
            Money.of("-200.00"),
            Money.of("500.00"),
            Money.of("200.00"),
            Money.of("300.00"),
            Money.of("300.00"),
            UPDATED_AT);

        AccountSummaryResponse response = AccountSummaryResponse.from(summary);

        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.balance()).isEqualTo(new BigDecimal("-200.00"));
        assertThat(response.creditLimit()).isEqualTo(new BigDecimal("500.00"));
        assertThat(response.usedLimit()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.availableLimit()).isEqualTo(new BigDecimal("300.00"));
        assertThat(response.availableFunds()).isEqualTo(new BigDecimal("300.00"));
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }
}
