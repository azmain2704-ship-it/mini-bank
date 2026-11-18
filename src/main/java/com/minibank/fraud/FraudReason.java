package com.minibank.fraud;

public enum FraudReason {
    /** A single transfer at or above the configured limit (default £5,000). */
    LARGE_AMOUNT,
    /** Too many transfers out of one account in a short time (default 5 within 60 seconds). */
    RAPID_TRANSFERS
}
