package com.mal.assignment.accounts.domain.models;

public record Authorization(
        String idempotencyKey,
        String referenceId,
        long requestedAmountInMinorUnits,
        AuthorizationStatus status,
        int heldFromDay
) {

    public boolean approved() {
        return status == AuthorizationStatus.APPROVED;
    }
}
