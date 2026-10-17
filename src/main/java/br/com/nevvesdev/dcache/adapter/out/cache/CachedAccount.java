package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;

import java.time.Instant;

public record CachedAccount(String id, String balance, String creditLimit, String updatedAt) {

    static CachedAccount from(Account account) {
        return new CachedAccount(
            account.id().toString(),
            account.balance().amount().toString(),
            account.creditLimit().amount().toString(),
            account.updatedAt().toString());
    }

    Account toDomain() {
        return new Account(
            AccountId.of(id),
            Balance.of(balance),
            CreditLimit.of(creditLimit),
            Instant.parse(updatedAt));
    }
}
