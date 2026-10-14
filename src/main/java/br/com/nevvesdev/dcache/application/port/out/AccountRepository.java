package br.com.nevvesdev.dcache.application.port.out;

import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;

import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findById(AccountId id);
}
