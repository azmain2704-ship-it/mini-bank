package com.minibank.transfer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.minibank.account.Account;
import com.minibank.fraud.FraudFlag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** One row in the transfer history. Rows are only ever added, never edited or deleted. */
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(name = "amount_pence", nullable = false)
    private long amountPence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "rejection_reason", length = 200)
    private String rejectionReason;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "transfer", fetch = FetchType.LAZY)
    private List<FraudFlag> fraudFlags = new ArrayList<>();

    protected Transfer() {
        // for JPA
    }

    private Transfer(Account from, Account to, long amountPence, TransferStatus status, String rejectionReason,
                     String idempotencyKey, Instant createdAt) {
        this.fromAccount = from;
        this.toAccount = to;
        this.amountPence = amountPence;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public static Transfer completed(Account from, Account to, long amountPence, String key, Instant at) {
        return new Transfer(from, to, amountPence, TransferStatus.COMPLETED, null, key, at);
    }

    public static Transfer rejected(Account from, Account to, long amountPence, String reason, String key, Instant at) {
        return new Transfer(from, to, amountPence, TransferStatus.REJECTED, reason, key, at);
    }

    public Long getId() {
        return id;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public long getAmountPence() {
        return amountPence;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<FraudFlag> getFraudFlags() {
        return fraudFlags;
    }
}
