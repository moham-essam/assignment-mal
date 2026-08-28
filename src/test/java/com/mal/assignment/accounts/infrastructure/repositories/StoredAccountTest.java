package com.mal.assignment.accounts.infrastructure.repositories;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Authorization;
import com.mal.assignment.accounts.domain.models.AuthorizationStatus;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoredAccountTest {

    @Test
    void snapshotDoesNotShareEntriesOrAuthorizationsWithTheWorkingAccount() {
        Account original = new Account("ACC-001", Currency.AED, 0L);
        original.appendLedgerEntry(LedgerEntry.draft("E1", 1, 120_000L, LedgerEntryType.CREDIT, "E1"));
        original.appendAuthorization(new Authorization(
                "E3", "Auth-A", 20_000L, AuthorizationStatus.APPROVED));
        Account snapshot = StoredAccount.snapshotOf(original);
        original.appendLedgerEntry(LedgerEntry.draft("E2", 1, 5_000L, LedgerEntryType.CREDIT, "E2"));
        original.appendAuthorization(new Authorization(
                "E8", "Auth-B", 1_000L, AuthorizationStatus.DECLINED));
        assertEquals(1, snapshot.entries().size());
        assertEquals(1, snapshot.authorizations().size());
        assertEquals(120_000L, snapshot.amountInMinorUnits());
        assertEquals(2, original.entries().size());
        assertEquals(2, original.authorizations().size());
    }
}
