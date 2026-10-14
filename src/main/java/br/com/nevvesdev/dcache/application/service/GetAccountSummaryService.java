package br.com.nevvesdev.dcache.application.service;

import br.com.nevvesdev.dcache.application.port.in.AccountSummary;
import br.com.nevvesdev.dcache.application.port.in.GetAccountSummaryUseCase;
import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.AccountNotFoundException;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;

public class GetAccountSummaryService implements GetAccountSummaryUseCase {

    private final AccountRepository repository;
    private final AccountCache cache;

    public GetAccountSummaryService(AccountRepository repository, AccountCache cache) {
        this.repository = Preconditions.requireNonNull(repository, "repository");
        this.cache = Preconditions.requireNonNull(cache, "cache");
    }

    @Override
    public AccountSummary execute(AccountId accountId) {
        Preconditions.requireNonNull(accountId, "accountId");
        Account account = cache.find(accountId).orElseGet(() -> loadAndCache(accountId));
        return AccountSummary.from(account);
    }

    private Account loadAndCache(AccountId accountId) {
        Account account = repository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
        cache.put(account);
        return account;
    }
}
