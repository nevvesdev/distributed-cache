package br.com.nevvesdev.dcache;

import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaEntity;
import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import io.micrometer.core.instrument.MeterRegistry;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=1",
    "spring.data.redis.timeout=300ms",
    "spring.data.redis.connect-timeout=300ms",
    "dcache.cache.resilience.failure-threshold=2",
    "dcache.cache.resilience.open-duration=1h"
})
@AutoConfigureMockMvc
@Import(PostgresContainerConfiguration.class)
class CacheOutageIntegrationTest {

    private static final String SUMMARY_URL = "/api/v1/accounts/{accountId}/summary";
    private static final Instant UPDATED_AT = Instant.parse("2026-10-17T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountJpaRepository jpaRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldKeepServingFromDatabaseWhileRedisIsUnavailable() throws Exception {
        UUID id = UUID.randomUUID();
        jpaRepository.saveAndFlush(new AccountJpaEntity(id, new BigDecimal("120.00"), new BigDecimal("500.00"), UPDATED_AT));

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(get(SUMMARY_URL, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(120.00))
                .andExpect(jsonPath("$.availableFunds").value(620.00));
        }
    }

    @Test
    void shouldStillReturnNotFoundForUnknownAccountWhileRedisIsUnavailable() throws Exception {
        mockMvc.perform(get(SUMMARY_URL, UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReportServiceUnavailableWhenEvictionCannotReachRedis() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/{accountId}/cache", UUID.randomUUID()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.title").value("Cache unavailable"));
    }

    @Test
    void shouldReportOpenCircuitThroughGauge() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(get(SUMMARY_URL, UUID.randomUUID())).andExpect(status().isNotFound());
        }

        assertThat(meterRegistry.get("dcache.cache.circuit.open").gauge().value()).isEqualTo(1.0);
    }
}
