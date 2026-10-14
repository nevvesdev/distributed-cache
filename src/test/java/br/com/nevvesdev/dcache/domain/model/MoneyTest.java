package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldNormalizeToTwoDecimalPlaces() {
        assertThat(Money.of("10.5").amount()).isEqualTo(new BigDecimal("10.50"));
        assertThat(Money.of("10").amount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldConsiderSameValueWithDifferentScaleAsEqual() {
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00"));
        assertThat(Money.of("10")).hasSameHashCodeAs(Money.of("10.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.001", "0.555", "1.234567"})
    void shouldRejectMoreThanTwoDecimalPlaces(String value) {
        assertThatThrownBy(() -> Money.of(value))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("at most 2 decimal places");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "abc", "10,50", "1.2.3"})
    void shouldRejectTextThatIsNotANumber(String value) {
        assertThatThrownBy(() -> Money.of(value))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("not a valid number");
    }

    @Test
    void shouldRejectNullValues() {
        assertThatThrownBy(() -> Money.of((String) null))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("must not be null");
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void shouldAddAndSubtract() {
        assertThat(Money.of("10.50").plus(Money.of("2.25"))).isEqualTo(Money.of("12.75"));
        assertThat(Money.of("10.50").minus(Money.of("12.00"))).isEqualTo(Money.of("-1.50"));
    }

    @Test
    void shouldRejectNullOperands() {
        assertThatThrownBy(() -> Money.ZERO.plus(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Money.ZERO.minus(null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldNegate() {
        assertThat(Money.of("5.00").negate()).isEqualTo(Money.of("-5.00"));
        assertThat(Money.ZERO.negate()).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldIdentifyNegativeValues() {
        assertThat(Money.of("-0.01").isNegative()).isTrue();
        assertThat(Money.ZERO.isNegative()).isFalse();
        assertThat(Money.of("0.01").isNegative()).isFalse();
    }

    @Test
    void shouldFloorNegativeValuesAtZero() {
        assertThat(Money.of("-10.00").nonNegative()).isEqualTo(Money.ZERO);
        assertThat(Money.of("10.00").nonNegative()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void shouldCompareByNumericValue() {
        assertThat(Money.of("10.00")).isGreaterThan(Money.of("9.99"));
        assertThat(Money.of("10.00")).isLessThan(Money.of("10.01"));
        assertThat(Money.of("10")).isEqualByComparingTo(Money.of("10.00"));
    }

    @Test
    void shouldPrintPlainString() {
        assertThat(Money.of("1E+2")).hasToString("100.00");
    }
}
