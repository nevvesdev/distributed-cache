package br.com.nevvesdev.dcache.application.port.out;

import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;

import java.util.Optional;

public interface AccountCache {

    Optional<Account> find(AccountId id);

    void put(Account account);

    void evict(AccountId id);
}
