package com.mal.assignment.accounts.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AccountPutIfAbsentTest {

    @Test
    void secondAppendWithSameIdempotencyKeyDoesNotChangeAmount() {
        Account account = new Account("ACC-001", Currency.AED, 0L);
        LedgerEntry first = account.appendLedgerEntry(LedgerEntry.draft(
                "E1", 1, 120_000L, LedgerEntryType.CREDIT, "E1"));
        LedgerEntry second = account.appendLedgerEntry(LedgerEntry.draft(
                "E1", 1, 120_000L, LedgerEntryType.CREDIT, "E1"));
        assertSame(first, second);
        assertEquals(120_000L, account.amountInMinorUnits());
        assertEquals(1, account.entries().size());
    }

    @Test
    void secondAuthorizationWithSameIdempotencyKeyDoesNotChangeHold() {
        Account account = new Account("ACC-001", Currency.AED, 25_000L);
        Authorization first = account.appendAuthorization(new Authorization(
                "E3", "Auth-A", 20_000L, AuthorizationStatus.APPROVED));
        Authorization second = account.appendAuthorization(new Authorization(
                "E3", "Auth-A", 20_000L, AuthorizationStatus.APPROVED));
        assertSame(first, second);
        assertEquals(20_000L, account.holdAmountInMinorUnits());
        assertEquals(1, account.authorizations().size());
    }
}
