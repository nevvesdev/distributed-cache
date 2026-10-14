package br.com.nevvesdev.dcache.domain.model;

import br.com.nevvesdev.dcache.domain.exception.DomainException;

public class AccountNotFoundException extends DomainException {

    private final AccountId accountId;

    public AccountNotFoundException(AccountId accountId) {
        super("Account not found: " + accountId);
        this.accountId = accountId;
    }

    public AccountId getAccountId() {
        return accountId;
    }
}
