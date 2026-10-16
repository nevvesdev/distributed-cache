package br.com.nevvesdev.dcache.adapter.in.web;

import br.com.nevvesdev.dcache.TestcontainersConfiguration;
import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaEntity;
import br.com.nevvesdev.dcache.adapter.out.persistence.AccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountApiIntegrationTest {

    private static final String SUMMARY_URL = "/api/v1/accounts/{accountId}/summary";
    private static final Instant UPDATED_AT = Instant.parse("2026-10-14T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountJpaRepository jpaRepository;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldReturnSummaryOfPersistedAccount() throws Exception {
        UUID id = UUID.randomUUID();
        jpaRepository.saveAndFlush(new AccountJpaEntity(id, new BigDecimal("-350.75"), new BigDecimal("1000.00"), UPDATED_AT));

        mockMvc.perform(get(SUMMARY_URL, id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(id.toString()))
            .andExpect(jsonPath("$.balance").value(-350.75))
            .andExpect(jsonPath("$.creditLimit").value(1000.00))
            .andExpect(jsonPath("$.usedLimit").value(350.75))
            .andExpect(jsonPath("$.availableLimit").value(649.25))
            .andExpect(jsonPath("$.availableFunds").value(649.25))
            .andExpect(jsonPath("$.updatedAt").value("2026-10-14T12:00:00Z"));
    }

    @Test
    void shouldReturnNotFoundForUnknownAccount() throws Exception {
        UUID unknown = UUID.randomUUID();

        mockMvc.perform(get(SUMMARY_URL, unknown))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Account not found"));
    }

    @Test
    void shouldReturnBadRequestForMalformedAccountId() throws Exception {
        mockMvc.perform(get(SUMMARY_URL, "123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"));
    }
}
