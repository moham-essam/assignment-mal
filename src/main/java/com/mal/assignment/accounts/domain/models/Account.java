package com.mal.assignment.accounts.domain.models;

import com.mal.assignment.accounts.domain.services.InterestPolicy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@AllArgsConstructor
public final class Account {

    private final String id;
    private final Currency currency;
    private final long openingBalanceInMinorUnits;
    private long amountInMinorUnits;
    private long holdAmountInMinorUnits;
    private Set<String> reversedReferenceIds;
    private Map<Integer, Long> accruedDailyInterestByDay;
    private long capitalizedInterestInMinorUnits;
    private long version;
    @Getter(AccessLevel.NONE)
    private Map<String, LedgerEntry> entries;
    @Getter(AccessLevel.NONE)
    private Map<String, Authorization> authorizations;

    public Account(String id, Currency currency, long openingBalanceInMinorUnits) {
        this(
                id,
                currency,
                openingBalanceInMinorUnits,
                openingBalanceInMinorUnits,
                0L,
                new HashSet<>(),
                new HashMap<>(),
                0L,
                0L,
                new HashMap<>(),
                new HashMap<>()
        );
    }

    public long availableBalanceInMinorUnits() {
        return amountInMinorUnits - holdAmountInMinorUnits;
    }

    public Collection<LedgerEntry> entries() {
        return List.copyOf(entries.values());
    }

    public Collection<Authorization> authorizations() {
        return List.copyOf(authorizations.values());
    }

    public void ensureSufficientAvailable(long requestedInMinorUnits) {
        if (availableBalanceInMinorUnits() < requestedInMinorUnits) {
            throw new InsufficientAvailableBalanceException(id, requestedInMinorUnits);
        }
    }

    public LedgerEntry appendLedgerEntry(LedgerEntry draft) {
        long before = amountInMinorUnits;
        long after = before + draft.signedAmountInMinorUnits();
        LedgerEntry row = draft.withBalances(before, after);
        LedgerEntry existing = entries.putIfAbsent(row.idempotencyKey(), row);
        if (existing != null) {
            return existing;
        }
        amountInMinorUnits = after;
        version++;
        if (draft.type() == LedgerEntryType.REVERSAL && draft.referenceId() != null) {
            reversedReferenceIds.add(draft.referenceId());
            version++;
        }
        if (draft.type() == LedgerEntryType.INTEREST_CAPITALIZATION) {
            capitalizedInterestInMinorUnits = draft.signedAmountInMinorUnits();
            version++;
        }
        return row;
    }

    public Authorization appendAuthorization(Authorization authorization) {
        Authorization existing = authorizations.putIfAbsent(
                authorization.idempotencyKey(), authorization);
        if (existing != null) {
            return existing;
        }
        if (authorization.approved()) {
            holdAmountInMinorUnits += authorization.requestedAmountInMinorUnits();
            version++;
        }
        return authorization;
    }

    public void releaseHold(long holdInMinorUnits) {
        long nextHold = holdAmountInMinorUnits - holdInMinorUnits;
        if (nextHold < 0L) {
            nextHold = 0L;
        }
        holdAmountInMinorUnits = nextHold;
        version++;
    }

    public void accrueInterestForDay(int day, InterestPolicy interestPolicy) {
        long closing = postingsOnlyClosing(day);
        long accrual = interestPolicy.dailyAccrual(closing);
        accrueInterestForDay(day, accrual);
    }

    public void accrueInterestForDay(int day, long accrualInMinorUnits) {
        accruedDailyInterestByDay.put(day, accrualInMinorUnits);
        version++;
    }

    public Optional<LedgerEntry> entryByReference(String referenceId) {
        return entries.values().stream()
                .filter(entry -> referenceId.equals(entry.referenceId()))
                .filter(entry -> entry.type() == LedgerEntryType.DEBIT || entry.type() == LedgerEntryType.CREDIT)
                .findFirst();
    }

    public Optional<Authorization> authorizationByReference(String referenceId) {
        return authorizations.values().stream()
                .filter(authorization -> referenceId.equals(authorization.referenceId()))
                .findFirst();
    }

    public List<LedgerEntry> ledgerEntriesSortedByValueDay() {
        List<LedgerEntry> ordered = new ArrayList<>(entries.values());
        ordered.sort(Comparator.comparingInt(LedgerEntry::valueDay)
                .thenComparing(LedgerEntry::idempotencyKey));
        return ordered;
    }

    public long closingBalance(int day) {
        return signedSumThrough(day, false);
    }

    public long postingsOnlyClosing(int day) {
        return signedSumThrough(day, true);
    }

    public long netFeeSignedAmount(int day) {
        return ledgerEntriesSortedByValueDay().stream()
                .filter(entry -> entry.valueDay() == day && entry.type().ownDayFee())
                .mapToLong(LedgerEntry::signedAmountInMinorUnits)
                .sum();
    }

    public boolean alreadyReversed(String referenceId) {
        return reversedReferenceIds.contains(referenceId);
    }

    public int lastClosedDay() {
        return accruedDailyInterestByDay.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public long summedAccruals() {
        return accruedDailyInterestByDay.values().stream().mapToLong(Long::longValue).sum();
    }

    private long signedSumThrough(int day, boolean excludeOwnDayFees) {
        List<LedgerEntry> ordered = ledgerEntriesSortedByValueDay();
        if (ordered.isEmpty()) {
            return openingBalanceInMinorUnits;
        }
        long running = ordered.getFirst().balanceBeforeInMinorUnits();
        for (LedgerEntry entry : ordered) {
            if (entry.valueDay() > day) {
                break;
            }
            if (excludeOwnDayFees && entry.valueDay() == day && entry.type().ownDayFee()) {
                continue;
            }
            if (entry.type() == LedgerEntryType.INTEREST_CAPITALIZATION && day < entry.valueDay()) {
                continue;
            }
            running += entry.signedAmountInMinorUnits();
        }
        return running;
    }
}
