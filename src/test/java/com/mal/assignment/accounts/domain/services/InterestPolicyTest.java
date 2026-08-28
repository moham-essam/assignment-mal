package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterestPolicyTest {

    @Test
    void dailyAccrualTruncatesDown() {
        InterestPolicy policy = new InterestPolicy(4, 10_000, RoundingMode.DOWN);
        assertEquals(10L, policy.dailyAccrual(25_000L));
        assertEquals(26L, policy.dailyAccrual(65_000L));
        assertEquals(18L, policy.dailyAccrual(46_500L));
        assertEquals(0L, policy.dailyAccrual(0L));
        assertEquals(0L, policy.dailyAccrual(-37_000L));
    }

    @Test
    void rejectsNonPositiveDenominatorAtConstruction() {
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new InterestPolicy(4, 0, RoundingMode.DOWN)
        );
        assertEquals(FailureReason.INVALID_DAILY_RATE, error.error().reason());
    }

    @Test
    void rejectsNegativeNumeratorAtConstruction() {
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new InterestPolicy(-1, 10_000, RoundingMode.DOWN)
        );
        assertEquals(FailureReason.INVALID_DAILY_RATE, error.error().reason());
    }

    @Test
    void rejectsUnsupportedRoundingAtConstruction() {
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new InterestPolicy(4, 10_000, RoundingMode.HALF_UP)
        );
        assertEquals(FailureReason.UNSUPPORTED_ROUNDING_MODE, error.error().reason());
    }
}
