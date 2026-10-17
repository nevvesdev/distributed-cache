package br.com.nevvesdev.dcache.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachePropertiesTest {

    @Test
    void shouldAcceptValidValues() {
        CacheProperties properties = new CacheProperties(true, Duration.ofMinutes(5), 0.1);

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.ttlJitter()).isEqualTo(0.1);
    }

    @Test
    void shouldRejectZeroTtl() {
        assertThatThrownBy(() -> new CacheProperties(true, Duration.ZERO, 0.1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive duration");
    }

    @Test
    void shouldRejectNegativeTtl() {
        assertThatThrownBy(() -> new CacheProperties(true, Duration.ofSeconds(-1), 0.1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullTtl() {
        assertThatThrownBy(() -> new CacheProperties(true, null, 0.1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectJitterOutsideZeroToOne() {
        assertThatThrownBy(() -> new CacheProperties(true, Duration.ofMinutes(5), -0.1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("between 0 and 1");
        assertThatThrownBy(() -> new CacheProperties(true, Duration.ofMinutes(5), 1.5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CacheProperties(true, Duration.ofMinutes(5), Double.NaN))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
