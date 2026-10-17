package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TtlPolicyTest {

    private static final Duration BASE = Duration.ofMinutes(10);

    @Test
    void shouldAlwaysReturnBaseWhenFixed() {
        TtlPolicy policy = TtlPolicy.fixed(BASE);

        assertThat(policy.next()).isEqualTo(BASE);
        assertThat(policy.next()).isEqualTo(BASE);
    }

    @Test
    void shouldAddFractionOfJitterBasedOnRandomValue() {
        TtlPolicy policy = new TtlPolicy(BASE, 0.1, () -> 0.5);

        assertThat(policy.next()).isEqualTo(BASE.plusSeconds(30));
    }

    @Test
    void shouldNotAddAnythingWhenRandomValueIsZero() {
        TtlPolicy policy = new TtlPolicy(BASE, 0.2, () -> 0.0);

        assertThat(policy.next()).isEqualTo(BASE);
    }

    @Test
    void shouldStayWithinJitterBoundsWithRealRandomness() {
        TtlPolicy policy = TtlPolicy.withJitter(BASE, 0.2);

        for (int i = 0; i < 1000; i++) {
            assertThat(policy.next().toMillis()).isBetween(BASE.toMillis(), BASE.plusMinutes(2).toMillis());
        }
    }

    @Test
    void shouldRejectNonPositiveBase() {
        assertThatThrownBy(() -> TtlPolicy.fixed(Duration.ZERO)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> TtlPolicy.fixed(Duration.ofSeconds(-1))).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldRejectJitterOutsideZeroToOne() {
        assertThatThrownBy(() -> TtlPolicy.withJitter(BASE, -0.1)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> TtlPolicy.withJitter(BASE, 1.1)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> TtlPolicy.withJitter(BASE, Double.NaN)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldRejectNullBase() {
        assertThatThrownBy(() -> TtlPolicy.fixed(null)).isInstanceOf(InvalidValueException.class);
    }
}
