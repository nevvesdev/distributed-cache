package br.com.nevvesdev.dcache.adapter.out.persistence;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountPersistenceMapperTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-14T12:00:00Z");

    @Test
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        AccountJpaEntity entity = new AccountJpaEntity(id, new BigDecimal("-200.00"), new BigDecimal("500.00"), UPDATED_AT);

        Account account = AccountPersistenceMapper.toDomain(entity);

        assertThat(account).isEqualTo(new Account(
            new AccountId(id), Balance.of("-200.00"), CreditLimit.of("500.00"), UPDATED_AT));
    }

    @Test
    void shouldRejectNegativeCreditLimitStoredInDatabase() {
        AccountJpaEntity entity = new AccountJpaEntity(
            UUID.randomUUID(), new BigDecimal("10.00"), new BigDecimal("-1.00"), UPDATED_AT);

        assertThatThrownBy(() -> AccountPersistenceMapper.toDomain(entity))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("must not be negative");
    }

    @Test
    void shouldRejectBalanceWithMoreThanTwoDecimalPlaces() {
        AccountJpaEntity entity = new AccountJpaEntity(
            UUID.randomUUID(), new BigDecimal("10.123"), new BigDecimal("1.00"), UPDATED_AT);

        assertThatThrownBy(() -> AccountPersistenceMapper.toDomain(entity))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("at most 2 decimal places");
    }
}
