package br.com.nevvesdev.dcache.adapter.in.web;

import br.com.nevvesdev.dcache.application.exception.CacheUnavailableException;
import br.com.nevvesdev.dcache.application.port.in.AccountSummary;
import br.com.nevvesdev.dcache.application.port.in.EvictAccountCacheUseCase;
import br.com.nevvesdev.dcache.application.port.in.GetAccountSummaryUseCase;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.AccountNotFoundException;
import br.com.nevvesdev.dcache.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String ACCOUNT_ID = "3f2b8c1e-9a4d-4e7b-8c55-1d2e3f4a5b6c";
    private static final String SUMMARY_URL = "/api/v1/accounts/{accountId}/summary";
    private static final String CACHE_URL = "/api/v1/accounts/{accountId}/cache";
    private static final Instant UPDATED_AT = Instant.parse("2026-10-14T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAccountSummaryUseCase getAccountSummary;

    @MockitoBean
    private EvictAccountCacheUseCase evictAccountCache;

    @Test
    void shouldReturnSummaryWhenAccountExists() throws Exception {
        AccountId id = AccountId.of(ACCOUNT_ID);
        when(getAccountSummary.execute(id)).thenReturn(new AccountSummary(
            id,
            Money.of("-200.00"),
            Money.of("500.00"),
            Money.of("200.00"),
            Money.of("300.00"),
            Money.of("300.00"),
            UPDATED_AT));

        mockMvc.perform(get(SUMMARY_URL, ACCOUNT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
            .andExpect(jsonPath("$.balance").value(-200.00))
            .andExpect(jsonPath("$.creditLimit").value(500.00))
            .andExpect(jsonPath("$.usedLimit").value(200.00))
            .andExpect(jsonPath("$.availableLimit").value(300.00))
            .andExpect(jsonPath("$.availableFunds").value(300.00))
            .andExpect(jsonPath("$.updatedAt").value("2026-10-14T12:00:00Z"));
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        AccountId id = AccountId.of(ACCOUNT_ID);
        when(getAccountSummary.execute(id)).thenThrow(new AccountNotFoundException(id));

        mockMvc.perform(get(SUMMARY_URL, ACCOUNT_ID))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Account not found"))
            .andExpect(jsonPath("$.detail").value("Account not found: " + ACCOUNT_ID));
    }

    @Test
    void shouldReturnBadRequestWhenAccountIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get(SUMMARY_URL, "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Invalid request"));

        verifyNoInteractions(getAccountSummary);
    }

    @Test
    void shouldHideInternalDetailsOnUnexpectedError() throws Exception {
        AccountId id = AccountId.of(ACCOUNT_ID);
        when(getAccountSummary.execute(id)).thenThrow(new IllegalStateException("database exploded"));

        mockMvc.perform(get(SUMMARY_URL, ACCOUNT_ID))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.title").value("Internal server error"))
            .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }

    @Test
    void shouldEvictCacheAndReturnNoContent() throws Exception {
        mockMvc.perform(delete(CACHE_URL, ACCOUNT_ID))
            .andExpect(status().isNoContent());

        verify(evictAccountCache).execute(AccountId.of(ACCOUNT_ID));
    }

    @Test
    void shouldReturnBadRequestWhenEvictingWithInvalidAccountId() throws Exception {
        mockMvc.perform(delete(CACHE_URL, "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"));

        verifyNoInteractions(evictAccountCache);
    }

    @Test
    void shouldReturnServiceUnavailableWhenCacheCannotBeEvicted() throws Exception {
        doThrow(new CacheUnavailableException("cache down", new IllegalStateException("redis")))
            .when(evictAccountCache).execute(AccountId.of(ACCOUNT_ID));

        mockMvc.perform(delete(CACHE_URL, ACCOUNT_ID))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.title").value("Cache unavailable"))
            .andExpect(jsonPath("$.detail").value("Cache is unavailable"));
    }
}
