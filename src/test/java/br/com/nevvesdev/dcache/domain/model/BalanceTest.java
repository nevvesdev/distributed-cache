package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceTest {

    @Test
    void shouldAcceptPositiveZeroAndNegativeAmounts() {
        assertThat(Balance.of("100.00").amount()).isEqualTo(Money.of("100.00"));
        assertThat(Balance.of("0").amount()).isEqualTo(Money.ZERO);
        assertThat(Balance.of("-50.00").amount()).isEqualTo(Money.of("-50.00"));
    }

    @Test
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> new Balance(null)).isInstanceOf(InvalidValueException.class);
    }
}
