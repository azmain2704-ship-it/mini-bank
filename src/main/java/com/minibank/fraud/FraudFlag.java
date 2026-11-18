package com.minibank.fraud;

import java.time.Instant;

import com.minibank.transfer.Transfer;

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
import jakarta.persistence.Table;

/** A note that a transfer looks suspicious and someone should look at it. It doesn't block the transfer. */
@Entity
@Table(name = "fraud_flags")
public class FraudFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FraudReason reason;

    @Column(nullable = false, length = 300)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FraudFlag() {
        // for JPA
    }

    public FraudFlag(Transfer transfer, FraudReason reason, String detail, Instant createdAt) {
        this.transfer = transfer;
        this.reason = reason;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public FraudReason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
