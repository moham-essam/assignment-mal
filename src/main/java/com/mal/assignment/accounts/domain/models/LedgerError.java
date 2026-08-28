package com.mal.assignment.accounts.domain.models;

public record LedgerError(
        FailureReason reason,
        String accountId,
        String message
) {
}
