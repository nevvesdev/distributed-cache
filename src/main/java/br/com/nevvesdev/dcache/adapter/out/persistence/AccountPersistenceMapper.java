package br.com.nevvesdev.dcache.adapter.out.persistence;

import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import br.com.nevvesdev.dcache.domain.model.Money;

final class AccountPersistenceMapper {

    private AccountPersistenceMapper() {
    }

    static Account toDomain(AccountJpaEntity entity) {
        return new Account(
            new AccountId(entity.getId()),
            new Balance(Money.of(entity.getBalance())),
            new CreditLimit(Money.of(entity.getCreditLimit())),
            entity.getUpdatedAt());
    }
}
