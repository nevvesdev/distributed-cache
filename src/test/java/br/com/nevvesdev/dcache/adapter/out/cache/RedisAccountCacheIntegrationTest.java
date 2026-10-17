package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.TestcontainersConfiguration;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RedisAccountCacheIntegrationTest {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Instant UPDATED_AT = Instant.parse("2026-10-16T09:00:00Z");

    private final StringRedisTemplate redisTemplate;
    private final RedisAccountCache cache;

    @Autowired
    RedisAccountCacheIntegrationTest(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.cache = new RedisAccountCache(redisTemplate, TtlPolicy.fixed(TTL));
    }

    private static Account anAccount(AccountId id, String balance) {
        return new Account(id, Balance.of(balance), CreditLimit.of("1000.00"), UPDATED_AT);
    }

    private static String keyOf(AccountId id) {
        return "dcache:account:v1:" + id;
    }

    @Test
    void shouldStoreAndRetrieveAccount() {
        Account account = anAccount(AccountId.generate(), "150.00");

        cache.put(account);

        assertThat(cache.find(account.id())).contains(account);
    }

    @Test
    void shouldReturnEmptyWhenKeyDoesNotExist() {
        assertThat(cache.find(AccountId.generate())).isEmpty();
    }

    @Test
    void shouldStoreEntryUnderVersionedKeyWithTtl() {
        Account account = anAccount(AccountId.generate(), "150.00");

        cache.put(account);

        String key = keyOf(account.id());
        assertThat(redisTemplate.hasKey(key)).isTrue();
        assertThat(redisTemplate.getExpire(key)).isNotNull().isBetween(1L, TTL.toSeconds());
    }

    @Test
    void shouldApplyJitterToTtl() {
        RedisAccountCache jitteredCache = new RedisAccountCache(redisTemplate, new TtlPolicy(TTL, 0.2, () -> 1.0));
        Account account = anAccount(AccountId.generate(), "150.00");

        jitteredCache.put(account);

        assertThat(redisTemplate.getExpire(keyOf(account.id())))
            .isNotNull()
            .isBetween(TTL.toSeconds() + 1, TTL.toSeconds() * 6 / 5);
    }

    @Test
    void shouldOverwriteExistingEntry() {
        AccountId id = AccountId.generate();
        cache.put(anAccount(id, "100.00"));

        cache.put(anAccount(id, "250.00"));

        assertThat(cache.find(id)).contains(anAccount(id, "250.00"));
    }

    @Test
    void shouldDiscardCorruptedEntryAndTreatAsMiss() {
        AccountId id = AccountId.generate();
        String key = keyOf(id);
        redisTemplate.opsForValue().set(key, "{not-json");

        assertThat(cache.find(id)).isEmpty();
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    void shouldDiscardEntryWithInvalidDomainData() {
        AccountId id = AccountId.generate();
        String key = keyOf(id);
        redisTemplate.opsForValue().set(key, "{\"id\":\"" + id + "\",\"balance\":\"10.00\",\"creditLimit\":\"-5.00\",\"updatedAt\":\"2026-10-16T09:00:00Z\"}");

        assertThat(cache.find(id)).isEmpty();
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    void shouldRejectNullArguments() {
        assertThatThrownBy(() -> cache.find(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> cache.put(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RedisAccountCache(null, TtlPolicy.fixed(TTL))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new RedisAccountCache(redisTemplate, null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> cache.evict(null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldEvictEntry() {
        Account account = anAccount(AccountId.generate(), "150.00");
        cache.put(account);

        cache.evict(account.id());

        assertThat(redisTemplate.hasKey(keyOf(account.id()))).isFalse();
        assertThat(cache.find(account.id())).isEmpty();
    }

    @Test
    void shouldIgnoreEvictionOfMissingEntry() {
        assertThatCode(() -> cache.evict(AccountId.generate())).doesNotThrowAnyException();
    }
}
