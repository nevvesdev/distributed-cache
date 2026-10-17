package br.com.nevvesdev.dcache.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheResiliencePropertiesTest {

    @Test
    void shouldAcceptValidValues() {
        CacheResilienceProperties properties = new CacheResilienceProperties(3, Duration.ofSeconds(30));

        assertThat(properties.failureThreshold()).isEqualTo(3);
        assertThat(properties.openDuration()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldRejectThresholdBelowOne() {
        assertThatThrownBy(() -> new CacheResilienceProperties(0, Duration.ofSeconds(30)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 1");
    }

    @Test
    void shouldRejectNonPositiveOpenDuration() {
        assertThatThrownBy(() -> new CacheResilienceProperties(3, Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CacheResilienceProperties(3, Duration.ofSeconds(-5)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CacheResilienceProperties(3, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
