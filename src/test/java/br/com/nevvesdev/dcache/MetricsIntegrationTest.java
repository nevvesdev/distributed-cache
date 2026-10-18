package br.com.nevvesdev.dcache;

import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaEntity;
import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MetricsIntegrationTest {

    private static final String SUMMARY_URL = "/api/v1/accounts/{accountId}/summary";
    private static final Instant UPDATED_AT = Instant.parse("2026-10-17T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountJpaRepository jpaRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    private double lookups(String result) {
        return meterRegistry.counter("dcache.cache.lookups", "result", result).count();
    }

    private long repositoryLookups(String outcome) {
        return meterRegistry.timer("dcache.repository.lookups", "outcome", outcome).count();
    }

    @Test
    void shouldCountMissThenHitAndQueryDatabaseOnlyOnce() throws Exception {
        UUID id = UUID.randomUUID();
        jpaRepository.saveAndFlush(new AccountJpaEntity(id, new BigDecimal("100.00"), new BigDecimal("500.00"), UPDATED_AT));
        double missesBefore = lookups("miss");
        double hitsBefore = lookups("hit");
        long foundBefore = repositoryLookups("found");

        mockMvc.perform(get(SUMMARY_URL, id)).andExpect(status().isOk());
        mockMvc.perform(get(SUMMARY_URL, id)).andExpect(status().isOk());

        assertThat(lookups("miss") - missesBefore).isEqualTo(1.0);
        assertThat(lookups("hit") - hitsBefore).isEqualTo(1.0);
        assertThat(repositoryLookups("found") - foundBefore).isEqualTo(1L);
    }

    @Test
    void shouldCountLookupsOfUnknownAccounts() throws Exception {
        long notFoundBefore = repositoryLookups("not_found");

        mockMvc.perform(get(SUMMARY_URL, UUID.randomUUID())).andExpect(status().isNotFound());

        assertThat(repositoryLookups("not_found") - notFoundBefore).isEqualTo(1L);
    }

    @Test
    void shouldExposeCustomMetricsThroughActuator() throws Exception {
        mockMvc.perform(get("/actuator/metrics/dcache.cache.lookups"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("dcache.cache.lookups"));

        mockMvc.perform(get("/actuator/metrics/dcache.cache.circuit.open"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.measurements[0].value").value(0.0));
    }
}
