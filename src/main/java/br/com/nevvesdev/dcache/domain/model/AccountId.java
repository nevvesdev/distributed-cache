package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;

import java.util.UUID;

/**
 * Unique identifier of an {@link Account}.
 */
public record AccountId(UUID value) {

    public AccountId {
        Preconditions.requireNonNull(value, "accountId");
    }

    public static AccountId of(String value) {
        Preconditions.requireNonNull(value, "accountId");
        try {
            return new AccountId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("accountId is not a valid UUID: " + value, e);
        }
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
