package br.com.nevvesdev.dcache;

import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaEntity;
import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CacheEvictionIntegrationTest {

    private static final String SUMMARY_URL = "/api/v1/accounts/{accountId}/summary";
    private static final String CACHE_URL = "/api/v1/accounts/{accountId}/cache";
    private static final Instant UPDATED_AT = Instant.parse("2026-10-17T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountJpaRepository jpaRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static String keyOf(UUID id) {
        return "dcache:account:v1:" + id;
    }

    private void storeAccount(UUID id, String balance) {
        jpaRepository.saveAndFlush(new AccountJpaEntity(id, new BigDecimal(balance), new BigDecimal("500.00"), UPDATED_AT));
    }

    @Test
    void shouldServeFreshDataAfterEvictionEndpointIsCalled() throws Exception {
        UUID id = UUID.randomUUID();
        storeAccount(id, "100.00");
        mockMvc.perform(get(SUMMARY_URL, id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(100.00));
        storeAccount(id, "999.00");
        mockMvc.perform(get(SUMMARY_URL, id))
            .andExpect(jsonPath("$.balance").value(100.00));

        mockMvc.perform(delete(CACHE_URL, id)).andExpect(status().isNoContent());

        assertThat(redisTemplate.hasKey(keyOf(id))).isFalse();
        mockMvc.perform(get(SUMMARY_URL, id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(999.00));
    }

    @Test
    void shouldReturnNoContentWhenThereIsNothingToEvict() throws Exception {
        mockMvc.perform(delete(CACHE_URL, UUID.randomUUID()))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectEvictionWithMalformedAccountId() throws Exception {
        mockMvc.perform(delete(CACHE_URL, "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
