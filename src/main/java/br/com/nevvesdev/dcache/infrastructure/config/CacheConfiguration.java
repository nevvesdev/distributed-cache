package br.com.nevvesdev.dcache.infrastructure.config;

import br.com.nevvesdev.dcache.adapter.out.cache.NoOpAccountCache;
import br.com.nevvesdev.dcache.adapter.out.cache.RedisAccountCache;
import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "dcache.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    AccountCache redisAccountCache(StringRedisTemplate redisTemplate, CacheProperties properties) {
        return new RedisAccountCache(redisTemplate, properties.ttl());
    }

    @Bean
    @ConditionalOnProperty(prefix = "dcache.cache", name = "enabled", havingValue = "false")
    AccountCache noOpAccountCache() {
        return new NoOpAccountCache();
    }
}
