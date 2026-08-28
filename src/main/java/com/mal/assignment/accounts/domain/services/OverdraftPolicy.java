package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class OverdraftPolicy {

    private final Map<Currency, Long> feesInMinorUnits;

    public OverdraftPolicy(Map<Currency, Long> feesInMinorUnits) {
        Objects.requireNonNull(feesInMinorUnits, "feesInMinorUnits");
        EnumMap<Currency, Long> copy = new EnumMap<>(Currency.class);
        for (Currency currency : Currency.values()) {
            Long fee = feesInMinorUnits.get(currency);
            if (fee == null) {
                throw LedgerDomainException.of(
                        FailureReason.MISSING_OVERDRAFT_FEE,
                        "No overdraft fee configured for " + currency
                );
            }
            if (fee <= 0L) {
                throw LedgerDomainException.of(
                        FailureReason.INVALID_OVERDRAFT_POLICY,
                        "Overdraft fee must be positive for " + currency
                );
            }
            copy.put(currency, fee);
        }
        this.feesInMinorUnits = Map.copyOf(copy);
    }

    public long feeInMinorUnits(Currency currency) {
        return feesInMinorUnits.get(currency);
    }
}
