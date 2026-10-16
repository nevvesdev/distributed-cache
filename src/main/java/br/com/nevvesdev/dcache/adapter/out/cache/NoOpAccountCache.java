package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoOpAccountCache implements AccountCache {

    @Override
    public Optional<Account> find(AccountId id) {
        return Optional.empty();
    }

    @Override
    public void put(Account account) {
    }
}
