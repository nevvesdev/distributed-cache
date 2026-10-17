package br.com.nevvesdev.dcache.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "dcache.cache.resilience")
public record CacheResilienceProperties(
    @DefaultValue("3") int failureThreshold,
    @DefaultValue("30s") Duration openDuration) {

    public CacheResilienceProperties {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("dcache.cache.resilience.failure-threshold must be at least 1");
        }
        if (openDuration == null || openDuration.isZero() || openDuration.isNegative()) {
            throw new IllegalArgumentException("dcache.cache.resilience.open-duration must be a positive duration");
        }
    }
}
