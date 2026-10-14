package br.com.nevvesdev.dcache.domain.shared;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;

public final class Preconditions {

    private Preconditions() {
    }

    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new InvalidValueException(name + " must not be null");
        }
        return value;
    }
}
