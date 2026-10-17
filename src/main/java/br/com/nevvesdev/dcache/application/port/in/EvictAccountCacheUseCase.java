package br.com.nevvesdev.dcache.application.port.in;

import br.com.nevvesdev.dcache.domain.model.AccountId;

public interface EvictAccountCacheUseCase {

    void execute(AccountId accountId);
}
