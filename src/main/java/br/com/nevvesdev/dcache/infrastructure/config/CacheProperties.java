package br.com.nevvesdev.dcache.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "dcache.cache")
public record CacheProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("5m") Duration ttl) {

    public CacheProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("dcache.cache.ttl must be a positive duration");
        }
    }
}
