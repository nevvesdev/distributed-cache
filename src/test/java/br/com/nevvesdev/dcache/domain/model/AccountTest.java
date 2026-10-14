package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    private static Account accountWith(String balance, String limit) {
        return new Account(AccountId.generate(), Balance.of(balance), CreditLimit.of(limit), NOW);
    }

    @ParameterizedTest(name = "balance={0}, limit={1} -> used={2}, availableLimit={3}, availableFunds={4}")
    @CsvSource({
        "100.00,  500.00, 0.00,   500.00, 600.00",
        "0.00,    500.00, 0.00,   500.00, 500.00",
        "-200.00, 500.00, 200.00, 300.00, 300.00",
        "-500.00, 500.00, 500.00, 0.00,   0.00",
        "-700.00, 500.00, 700.00, 0.00,   0.00",
        "100.00,  0.00,   0.00,   0.00,   100.00",
        "-50.00,  0.00,   50.00,  0.00,   0.00"
    })
    void shouldDeriveLimitsFromBalanceAndCreditLimit(
        String balance, String limit, String used, String availableLimit, String availableFunds) {

        Account account = accountWith(balance, limit);

        assertThat(account.usedLimit()).isEqualTo(Money.of(used));
        assertThat(account.availableLimit()).isEqualTo(Money.of(availableLimit));
        assertThat(account.availableFunds()).isEqualTo(Money.of(availableFunds));
    }

    @Test
    void shouldExposeInformedData() {
        AccountId id = AccountId.generate();

        Account account = new Account(id, Balance.of("10.00"), CreditLimit.of("20.00"), NOW);

        assertThat(account.id()).isEqualTo(id);
        assertThat(account.balance()).isEqualTo(Balance.of("10.00"));
        assertThat(account.creditLimit()).isEqualTo(CreditLimit.of("20.00"));
        assertThat(account.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectNullComponents() {
        AccountId id = AccountId.generate();
        Balance balance = Balance.of("1.00");
        CreditLimit limit = CreditLimit.of("1.00");

        assertThatThrownBy(() -> new Account(null, balance, limit, NOW)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Account(id, null, limit, NOW)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Account(id, balance, null, NOW)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Account(id, balance, limit, null)).isInstanceOf(InvalidValueException.class);
    }
}
