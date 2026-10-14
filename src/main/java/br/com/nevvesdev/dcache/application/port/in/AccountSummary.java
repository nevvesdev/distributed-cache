package br.com.nevvesdev.dcache.application.port.in;

import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Money;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;

import java.time.Instant;

public record AccountSummary(
    AccountId accountId,
    Money balance,
    Money creditLimit,
    Money usedLimit,
    Money availableLimit,
    Money availableFunds,
    Instant updatedAt) {

    public static AccountSummary from(Account account) {
        Preconditions.requireNonNull(account, "account");
        return new AccountSummary(
            account.id(),
            account.balance().amount(),
            account.creditLimit().amount(),
            account.usedLimit(),
            account.availableLimit(),
            account.availableFunds(),
            account.updatedAt());
    }
}
