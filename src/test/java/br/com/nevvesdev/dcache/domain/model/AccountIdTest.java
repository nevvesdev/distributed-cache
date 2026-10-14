package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountIdTest {

    private static final String VALID_UUID = "3f2b8c1e-9a4d-4e7b-8c55-1d2e3f4a5b6c";

    @Test
    void shouldCreateFromValidUuidText() {
        AccountId id = AccountId.of(VALID_UUID);

        assertThat(id.value()).isEqualTo(UUID.fromString(VALID_UUID));
        assertThat(id).hasToString(VALID_UUID);
    }

    @Test
    void shouldIgnoreSurroundingWhitespace() {
        assertThat(AccountId.of("  " + VALID_UUID + "  ")).isEqualTo(AccountId.of(VALID_UUID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-uuid", "12345"})
    void shouldRejectInvalidUuidText(String value) {
        assertThatThrownBy(() -> AccountId.of(value))
            .isInstanceOf(InvalidValueException.class)
            .hasMessageContaining("not a valid UUID");
    }

    @Test
    void shouldRejectNullValues() {
        assertThatThrownBy(() -> AccountId.of(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new AccountId(null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldGenerateDistinctIds() {
        assertThat(AccountId.generate()).isNotEqualTo(AccountId.generate());
    }
}
