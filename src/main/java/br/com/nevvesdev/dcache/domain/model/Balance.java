package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.shared.Preconditions;

public record Balance(Money amount) {

    public Balance {
        Preconditions.requireNonNull(amount, "balance");
    }

    public static Balance of(String amount) {
        return new Balance(Money.of(amount));
    }
}
