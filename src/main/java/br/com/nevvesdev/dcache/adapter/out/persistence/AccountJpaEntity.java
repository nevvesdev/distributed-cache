package br.com.nevvesdev.dcache.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountJpaEntity() {
    }

    public AccountJpaEntity(UUID id, BigDecimal balance, BigDecimal creditLimit, Instant updatedAt) {
        this.id = id;
        this.balance = balance;
        this.creditLimit = creditLimit;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
