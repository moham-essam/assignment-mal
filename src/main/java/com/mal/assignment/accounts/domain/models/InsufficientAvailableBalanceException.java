package com.mal.assignment.accounts.domain.models;

import lombok.Getter;

@Getter
public final class InsufficientAvailableBalanceException extends LedgerDomainException {

    private final String accountId;
    private final long requestedInMinorUnits;

    public InsufficientAvailableBalanceException(String accountId, long requestedInMinorUnits) {
        super(new LedgerError(
                FailureReason.INSUFFICIENT_AVAILABLE_BALANCE,
                accountId,
                "Insufficient available balance on " + accountId + " for " + requestedInMinorUnits
        ));
        this.accountId = accountId;
        this.requestedInMinorUnits = requestedInMinorUnits;
    }
}
