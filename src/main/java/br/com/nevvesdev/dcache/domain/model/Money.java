package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int SCALE = 2;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Preconditions.requireNonNull(amount, "amount");
        try {
            amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidValueException(
                "amount must have at most " + SCALE + " decimal places: " + amount.toPlainString(), e);
        }
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        Preconditions.requireNonNull(amount, "amount");
        try {
            return new Money(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new InvalidValueException("amount is not a valid number: " + amount, e);
        }
    }

    public Money plus(Money other) {
        Preconditions.requireNonNull(other, "other");
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        Preconditions.requireNonNull(other, "other");
        return new Money(amount.subtract(other.amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public Money nonNegative() {
        return isNegative() ? ZERO : this;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
