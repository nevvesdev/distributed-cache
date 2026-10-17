package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

public class RedisAccountCache implements AccountCache {

    private static final Logger log = LoggerFactory.getLogger(RedisAccountCache.class);
    private static final String KEY_PREFIX = "dcache:account:v1:";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final StringRedisTemplate redisTemplate;
    private final TtlPolicy ttlPolicy;

    public RedisAccountCache(StringRedisTemplate redisTemplate, TtlPolicy ttlPolicy) {
        this.redisTemplate = Preconditions.requireNonNull(redisTemplate, "redisTemplate");
        this.ttlPolicy = Preconditions.requireNonNull(ttlPolicy, "ttlPolicy");
    }

    @Override
    public Optional<Account> find(AccountId id) {
        Preconditions.requireNonNull(id, "id");
        String key = keyOf(id);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        return deserialize(key, json);
    }

    @Override
    public void put(Account account) {
        Preconditions.requireNonNull(account, "account");
        String json = MAPPER.writeValueAsString(CachedAccount.from(account));
        redisTemplate.opsForValue().set(keyOf(account.id()), json, ttlPolicy.next());
    }

    @Override
    public void evict(AccountId id) {
        Preconditions.requireNonNull(id, "id");
        redisTemplate.delete(keyOf(id));
    }

    private Optional<Account> deserialize(String key, String json) {
        try {
            return Optional.of(MAPPER.readValue(json, CachedAccount.class).toDomain());
        } catch (RuntimeException exception) {
            log.warn("Discarding unreadable cache entry {}", key, exception);
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    private static String keyOf(AccountId id) {
        return KEY_PREFIX + id;
    }
}
