package com.minibank.transfer;

public enum TransferStatus {
    COMPLETED,
    /** Kept in the history on purpose: a refused transfer is still something that happened. */
    REJECTED
}
