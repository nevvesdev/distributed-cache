package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.shared.Preconditions;

import java.time.Instant;

public record Account(AccountId id, Balance balance, CreditLimit creditLimit, Instant updatedAt) {

    public Account {
        Preconditions.requireNonNull(id, "id");
        Preconditions.requireNonNull(balance, "balance");
        Preconditions.requireNonNull(creditLimit, "creditLimit");
        Preconditions.requireNonNull(updatedAt, "updatedAt");
    }

    public Money usedLimit() {
        Money current = balance.amount();
        return current.isNegative() ? current.negate() : Money.ZERO;
    }

    public Money availableLimit() {
        return creditLimit.amount().minus(usedLimit()).nonNegative();
    }

    public Money availableFunds() {
        return balance.amount().plus(creditLimit.amount()).nonNegative();
    }
}
