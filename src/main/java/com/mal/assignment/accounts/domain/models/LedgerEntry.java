package com.mal.assignment.accounts.domain.models;

public record LedgerEntry(
        String idempotencyKey,
        int valueDay,
        long signedAmountInMinorUnits,
        LedgerEntryType type,
        String referenceId,
        long balanceBeforeInMinorUnits,
        long balanceAfterInMinorUnits
) {

    public LedgerEntry withBalances(long balanceBeforeInMinorUnits, long balanceAfterInMinorUnits) {
        return new LedgerEntry(
                idempotencyKey,
                valueDay,
                signedAmountInMinorUnits,
                type,
                referenceId,
                balanceBeforeInMinorUnits,
                balanceAfterInMinorUnits
        );
    }

    public static LedgerEntry draft(
            String idempotencyKey,
            int valueDay,
            long signedAmountInMinorUnits,
            LedgerEntryType type,
            String referenceId
    ) {
        return new LedgerEntry(
                idempotencyKey,
                valueDay,
                signedAmountInMinorUnits,
                type,
                referenceId,
                0L,
                0L
        );
    }
}
