package br.com.nevvesdev.dcache.adapter.in.web;

import br.com.nevvesdev.dcache.application.port.in.GetAccountSummaryUseCase;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final GetAccountSummaryUseCase getAccountSummary;

    public AccountController(GetAccountSummaryUseCase getAccountSummary) {
        this.getAccountSummary = getAccountSummary;
    }

    @GetMapping("/{accountId}/summary")
    public AccountSummaryResponse getSummary(@PathVariable("accountId") String accountId) {
        return AccountSummaryResponse.from(getAccountSummary.execute(AccountId.of(accountId)));
    }
}
