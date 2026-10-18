package br.com.nevvesdev.dcache.infrastructure.config;

import br.com.nevvesdev.dcache.adapter.out.cache.FailSafeAccountCache;
import br.com.nevvesdev.dcache.adapter.out.cache.NoOpAccountCache;
import br.com.nevvesdev.dcache.adapter.out.cache.RedisAccountCache;
import br.com.nevvesdev.dcache.adapter.out.cache.TtlPolicy;
import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.infrastructure.observability.InstrumentedAccountCache;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CacheProperties.class, CacheResilienceProperties.class})
public class CacheConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "dcache.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    AccountCache redisAccountCache(
        StringRedisTemplate redisTemplate,
        CacheProperties cache,
        CacheResilienceProperties resilience,
        MeterRegistry meterRegistry) {
        TtlPolicy ttlPolicy = TtlPolicy.withJitter(cache.ttl(), cache.ttlJitter());
        AccountCache redis = new InstrumentedAccountCache(new RedisAccountCache(redisTemplate, ttlPolicy), meterRegistry);
        FailSafeAccountCache failSafe =
            new FailSafeAccountCache(redis, resilience.failureThreshold(), resilience.openDuration());
        Gauge.builder("dcache.cache.circuit.open", failSafe, safeCache -> safeCache.isCircuitOpen() ? 1 : 0)
            .description("1 when the cache circuit breaker is open, 0 when it is closed")
            .register(meterRegistry);
        return failSafe;
    }

    @Bean
    @ConditionalOnProperty(prefix = "dcache.cache", name = "enabled", havingValue = "false")
    AccountCache noOpAccountCache() {
        return new NoOpAccountCache();
    }
}
