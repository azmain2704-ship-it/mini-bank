package com.minibank.common;

/** The request breaks a rule before we even touch the database (e.g. sending money to the same account). Becomes a 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
