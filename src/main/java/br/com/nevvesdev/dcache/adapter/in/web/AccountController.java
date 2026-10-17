package br.com.nevvesdev.dcache.adapter.in.web;

import br.com.nevvesdev.dcache.application.port.in.EvictAccountCacheUseCase;
import br.com.nevvesdev.dcache.application.port.in.GetAccountSummaryUseCase;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final GetAccountSummaryUseCase getAccountSummary;
    private final EvictAccountCacheUseCase evictAccountCache;

    public AccountController(GetAccountSummaryUseCase getAccountSummary, EvictAccountCacheUseCase evictAccountCache) {
        this.getAccountSummary = getAccountSummary;
        this.evictAccountCache = evictAccountCache;
    }

    @GetMapping("/{accountId}/summary")
    public AccountSummaryResponse getSummary(@PathVariable("accountId") String accountId) {
        return AccountSummaryResponse.from(getAccountSummary.execute(AccountId.of(accountId)));
    }

    @DeleteMapping("/{accountId}/cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void evictCache(@PathVariable("accountId") String accountId) {
        evictAccountCache.execute(AccountId.of(accountId));
    }
}
