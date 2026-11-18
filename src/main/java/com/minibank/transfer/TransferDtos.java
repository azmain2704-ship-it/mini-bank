package com.minibank.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.minibank.common.Money;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public final class TransferDtos {

    private TransferDtos() {
    }

    public record TransferRequest(
            @NotNull Long fromAccountId,
            @NotNull Long toAccountId,
            @NotNull @DecimalMin(value = "0.01", message = "must be more than zero")
            @Digits(integer = 12, fraction = 2, message = "must be pounds and pence, e.g. 12.50") BigDecimal amount) {
    }

    public record TransferResponse(Long id, Long fromAccountId, Long toAccountId, BigDecimal amount, TransferStatus status,
                                   String rejectionReason, List<String> fraudFlags, Instant createdAt) {
        public static TransferResponse from(Transfer t) {
            return new TransferResponse(t.getId(), t.getFromAccount().getId(), t.getToAccount().getId(),
                    Money.toPounds(t.getAmountPence()), t.getStatus(), t.getRejectionReason(),
                    t.getFraudFlags().stream().map(f -> f.getReason().name()).toList(), t.getCreatedAt());
        }
    }

    /** The service's answer: the transfer, plus whether it was a repeat of an earlier request with the same key. */
    public record TransferOutcome(TransferResponse transfer, boolean replayed) {
    }
}
