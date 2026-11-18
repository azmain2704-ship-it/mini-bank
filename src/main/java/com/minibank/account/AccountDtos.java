package com.minibank.account;

import java.math.BigDecimal;
import java.time.Instant;

import com.minibank.common.Money;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** What goes in and out of the API for accounts. Amounts are pounds, e.g. 12.50. */
public final class AccountDtos {

    private AccountDtos() {
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 100) String ownerName,
            @NotNull @DecimalMin(value = "0.00", message = "can't be negative")
            @Digits(integer = 12, fraction = 2, message = "must be pounds and pence, e.g. 12.50") BigDecimal openingBalance) {
    }

    public record DepositRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be more than zero")
            @Digits(integer = 12, fraction = 2, message = "must be pounds and pence, e.g. 12.50") BigDecimal amount) {
    }

    public record AccountResponse(Long id, String ownerName, BigDecimal balance, Instant createdAt) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getOwnerName(), Money.toPounds(a.getBalancePence()), a.getCreatedAt());
        }
    }
}
