package br.com.nevvesdev.dcache.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachePropertiesTest {

    @Test
    void shouldAcceptPositiveTtl() {
        CacheProperties properties = new CacheProperties(true, Duration.ofMinutes(5));

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldRejectZeroTtl() {
        assertThatThrownBy(() -> new CacheProperties(true, Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive duration");
    }

    @Test
    void shouldRejectNegativeTtl() {
        assertThatThrownBy(() -> new CacheProperties(true, Duration.ofSeconds(-1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullTtl() {
        assertThatThrownBy(() -> new CacheProperties(true, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
