package com.minibank.common;

/** An Idempotency-Key that was already used, sent again with a different transfer. Becomes a 422. */
public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException(String message) {
        super(message);
    }
}
