package com.mal.assignment.accounts.domain.models;

/**
 * Append-only journal row. {@code balanceBeforeInMinorUnits} / {@code balanceAfterInMinorUnits}
 * are the booked amount at append time. They match a day's close only while this row is the
 * last entry of that value day; a later append with the same or an earlier {@code valueDay}
 * leaves them stale. Closing uses opening plus signed amounts through {@code valueDay}.
 */
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
