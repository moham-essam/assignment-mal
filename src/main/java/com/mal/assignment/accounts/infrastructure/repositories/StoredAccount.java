package com.mal.assignment.accounts.infrastructure.repositories;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Authorization;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public final class StoredAccount {

    private final AtomicReference<Account> snapshot;

    public static StoredAccount from(Account account) {
        return new StoredAccount(new AtomicReference<>(snapshotOf(account)));
    }

    public Account workingCopy() {
        return snapshotOf(snapshot.get());
    }

    public Account committed() {
        return snapshot.get();
    }

    public boolean compareAndSet(Account expected, Account next) {
        return snapshot.compareAndSet(expected, snapshotOf(next));
    }

    static Account snapshotOf(Account account) {
        Map<String, LedgerEntry> entries = new HashMap<>();
        for (LedgerEntry entry : account.entries()) {
            entries.put(entry.idempotencyKey(), entry);
        }
        Map<String, Authorization> authorizations = new HashMap<>();
        for (Authorization authorization : account.authorizations()) {
            authorizations.put(authorization.idempotencyKey(), authorization);
        }
        return new Account(
                account.id(),
                account.currency(),
                account.openingBalanceInMinorUnits(),
                account.amountInMinorUnits(),
                account.holdAmountInMinorUnits(),
                new HashSet<>(account.reversedReferenceIds()),
                new HashMap<>(account.accruedDailyInterestByDay()),
                account.capitalizedInterestInMinorUnits(),
                account.version(),
                entries,
                authorizations
        );
    }
}
