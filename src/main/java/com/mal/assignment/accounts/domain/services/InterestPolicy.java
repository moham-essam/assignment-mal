package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;

import java.math.RoundingMode;

public final class InterestPolicy {

    private final int dailyRateNumerator;
    private final int dailyRateDenominator;

    public InterestPolicy(int dailyRateNumerator, int dailyRateDenominator, RoundingMode roundingMode) {
        if (dailyRateNumerator < 0) {
            throw LedgerDomainException.of(
                    FailureReason.INVALID_DAILY_RATE,
                    "dailyRateNumerator must be non-negative"
            );
        }
        if (dailyRateDenominator <= 0) {
            throw LedgerDomainException.of(
                    FailureReason.INVALID_DAILY_RATE,
                    "dailyRateDenominator must be positive"
            );
        }
        if (roundingMode != RoundingMode.DOWN) {
            throw LedgerDomainException.of(
                    FailureReason.UNSUPPORTED_ROUNDING_MODE,
                    "Interest rounding must be DOWN, was " + roundingMode
            );
        }
        this.dailyRateNumerator = dailyRateNumerator;
        this.dailyRateDenominator = dailyRateDenominator;
    }

    public long dailyAccrual(long postingsOnlyClosingInMinorUnits) {
        if (postingsOnlyClosingInMinorUnits <= 0) {
            return 0L;
        }
        return (postingsOnlyClosingInMinorUnits * (long) dailyRateNumerator) / dailyRateDenominator;
    }
}
