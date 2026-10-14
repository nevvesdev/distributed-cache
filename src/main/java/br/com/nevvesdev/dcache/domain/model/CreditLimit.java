package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;

public record CreditLimit(Money amount) {

    public CreditLimit {
        Preconditions.requireNonNull(amount, "creditLimit");
        if (amount.isNegative()) {
            throw new InvalidValueException("creditLimit must not be negative: " + amount);
        }
    }

    public static CreditLimit of(String amount) {
        return new CreditLimit(Money.of(amount));
    }
}
