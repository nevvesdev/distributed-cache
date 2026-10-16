package br.com.nevvesdev.dcache.adapter.in.web;

import br.com.nevvesdev.dcache.application.port.in.AccountSummary;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountSummaryResponse(
    String accountId,
    BigDecimal balance,
    BigDecimal creditLimit,
    BigDecimal usedLimit,
    BigDecimal availableLimit,
    BigDecimal availableFunds,
    Instant updatedAt) {

    public static AccountSummaryResponse from(AccountSummary summary) {
        return new AccountSummaryResponse(
            summary.accountId().toString(),
            summary.balance().amount(),
            summary.creditLimit().amount(),
            summary.usedLimit().amount(),
            summary.availableLimit().amount(),
            summary.availableFunds().amount(),
            summary.updatedAt());
    }
}
