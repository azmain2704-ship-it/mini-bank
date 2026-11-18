package com.minibank.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    /** Whole pence. The database also has CHECK (balance_pence >= 0) as a last line of defence. */
    @Column(name = "balance_pence", nullable = false)
    private long balancePence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Account() {
        // for JPA
    }

    public Account(String ownerName, long openingBalancePence, Instant createdAt) {
        if (openingBalancePence < 0) {
            throw new IllegalArgumentException("Opening balance can't be negative");
        }
        this.ownerName = ownerName;
        this.balancePence = openingBalancePence;
        this.createdAt = createdAt;
    }

    public void deposit(long pence) {
        if (pence <= 0) {
            throw new IllegalArgumentException("Deposit must be positive");
        }
        balancePence = Math.addExact(balancePence, pence);
    }

    public boolean canAfford(long pence) {
        return pence <= balancePence;
    }

    public void withdraw(long pence) {
        if (pence <= 0) {
            throw new IllegalArgumentException("Withdrawal must be positive");
        }
        if (!canAfford(pence)) {
            throw new IllegalStateException("Insufficient funds");
        }
        balancePence -= pence;
    }

    public Long getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getBalancePence() {
        return balancePence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
