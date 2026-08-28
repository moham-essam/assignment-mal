package com.mal.assignment.accounts.domain.models;

import lombok.Getter;

@Getter
public class LedgerDomainException extends RuntimeException {

    private final LedgerError error;

    public LedgerDomainException(LedgerError error) {
        super(error.message());
        this.error = error;
    }

    public LedgerDomainException(LedgerError error, Throwable cause) {
        super(error.message(), cause);
        this.error = error;
    }

    public static LedgerDomainException of(FailureReason reason, String message) {
        return new LedgerDomainException(new LedgerError(reason, null, message));
    }

    public static LedgerDomainException of(FailureReason reason, String accountId, String message) {
        return new LedgerDomainException(new LedgerError(reason, accountId, message));
    }

    public static LedgerDomainException of(FailureReason reason, String message, Throwable cause) {
        return new LedgerDomainException(new LedgerError(reason, null, message), cause);
    }
}
