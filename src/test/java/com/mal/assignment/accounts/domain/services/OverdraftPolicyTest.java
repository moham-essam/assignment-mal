package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OverdraftPolicyTest {

    @Test
    void feesComeFromInitializedMap() {
        OverdraftPolicy policy = new OverdraftPolicy(Map.of(
                Currency.AED, 2500L,
                Currency.BHD, 25_000L
        ));
        assertEquals(2500L, policy.feeInMinorUnits(Currency.AED));
        assertEquals(25_000L, policy.feeInMinorUnits(Currency.BHD));
    }

    @Test
    void rejectsMissingCurrencyAtConstruction() {
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new OverdraftPolicy(Map.of(Currency.AED, 2500L))
        );
        assertEquals(FailureReason.MISSING_OVERDRAFT_FEE, error.error().reason());
    }

    @Test
    void rejectsNonPositiveFeeAtConstruction() {
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> new OverdraftPolicy(Map.of(Currency.AED, 0L, Currency.BHD, 25_000L))
        );
        assertEquals(FailureReason.INVALID_OVERDRAFT_POLICY, error.error().reason());
    }
}
