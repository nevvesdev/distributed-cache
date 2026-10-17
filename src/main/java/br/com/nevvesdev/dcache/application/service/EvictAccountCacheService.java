package br.com.nevvesdev.dcache.application.service;

import br.com.nevvesdev.dcache.application.port.in.EvictAccountCacheUseCase;
import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;

public class EvictAccountCacheService implements EvictAccountCacheUseCase {

    private final AccountCache cache;

    public EvictAccountCacheService(AccountCache cache) {
        this.cache = Preconditions.requireNonNull(cache, "cache");
    }

    @Override
    public void execute(AccountId accountId) {
        Preconditions.requireNonNull(accountId, "accountId");
        cache.evict(accountId);
    }
}
