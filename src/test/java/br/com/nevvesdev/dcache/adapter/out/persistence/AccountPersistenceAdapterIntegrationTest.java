package br.com.nevvesdev.dcache.adapter.out.persistence;

import br.com.nevvesdev.dcache.TestcontainersConfiguration;
import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountPersistenceAdapterIntegrationTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-10-14T12:00:00Z");

    private final AccountRepository accountRepository;
    private final AccountJpaRepository jpaRepository;

    @Autowired
    AccountPersistenceAdapterIntegrationTest(AccountRepository accountRepository, AccountJpaRepository jpaRepository) {
        this.accountRepository = accountRepository;
        this.jpaRepository = jpaRepository;
    }

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldReturnAccountWhenItExists() {
        UUID id = UUID.randomUUID();
        jpaRepository.saveAndFlush(new AccountJpaEntity(id, new BigDecimal("150.00"), new BigDecimal("1000.00"), UPDATED_AT));

        Optional<Account> result = accountRepository.findById(new AccountId(id));

        assertThat(result).contains(new Account(
            new AccountId(id), Balance.of("150.00"), CreditLimit.of("1000.00"), UPDATED_AT));
    }

    @Test
    void shouldPreserveNegativeBalance() {
        UUID id = UUID.randomUUID();
        jpaRepository.saveAndFlush(new AccountJpaEntity(id, new BigDecimal("-320.45"), new BigDecimal("500.00"), UPDATED_AT));

        Optional<Account> result = accountRepository.findById(new AccountId(id));

        assertThat(result).isPresent();
        assertThat(result.get().balance()).isEqualTo(Balance.of("-320.45"));
    }

    @Test
    void shouldReturnEmptyWhenAccountDoesNotExist() {
        Optional<Account> result = accountRepository.findById(AccountId.generate());

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() -> accountRepository.findById(null))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("id must not be null");
    }
}
