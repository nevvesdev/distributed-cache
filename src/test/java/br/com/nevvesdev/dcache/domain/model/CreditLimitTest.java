package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditLimitTest {

    @Test
    void shouldAcceptZeroAndPositiveAmounts() {
        assertThat(CreditLimit.of("0").amount()).isEqualTo(Money.ZERO);
        assertThat(CreditLimit.of("500.00").amount()).isEqualTo(Money.of("500.00"));
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> CreditLimit.of("-0.01"))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("must not be negative");
    }

    @Test
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> new CreditLimit(null)).isInstanceOf(InvalidValueException.class);
    }
}
