package com.mal.assignment.accounts.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinorUnitsTest {

    @Test
    void splitEvenlyAssignsRemainderToFirstInstalments() {
        assertArrayEquals(new long[] {3334L, 3333L, 3333L}, MinorUnits.splitEvenly(10_000L, 3));
    }

    @Test
    void splitEvenlyPreservesExactTotal() {
        long[] split = MinorUnits.splitEvenly(10_000L, 3);
        assertEquals(10_000L, split[0] + split[1] + split[2]);
    }

    @Test
    void splitEvenlyRejectsNonPositiveParts() {
        LedgerDomainException error = assertThrows(LedgerDomainException.class, () -> MinorUnits.splitEvenly(100L, 0));
        assertEquals(FailureReason.INVALID_SPLIT_PARTS, error.error().reason());
    }

    @Test
    void formatUsesCurrencyScale() {
        assertEquals("250.00", MinorUnits.format(25_000L, Currency.AED));
        assertEquals("10.008", MinorUnits.format(10_008L, Currency.BHD));
        assertEquals("-3.70", MinorUnits.format(-370L, Currency.AED));
    }
}
