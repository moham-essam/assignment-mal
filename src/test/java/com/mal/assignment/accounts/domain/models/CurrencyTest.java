package com.mal.assignment.accounts.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyTest {

    @Test
    void aedHasTwoDecimalPlaces() {
        assertEquals(2, Currency.AED.scale());
    }

    @Test
    void bhdHasThreeDecimalPlaces() {
        assertEquals(3, Currency.BHD.scale());
    }

    @Test
    void fromCodeParsesKnownCurrencies() {
        assertEquals(Currency.AED, Currency.fromCode("aed"));
        assertEquals(Currency.BHD, Currency.fromCode("BHD"));
    }

    @Test
    void fromCodeRejectsUnknownCurrencies() {
        LedgerDomainException error = assertThrows(LedgerDomainException.class, () -> Currency.fromCode("USD"));
        assertEquals(FailureReason.UNKNOWN_CURRENCY, error.error().reason());
    }
}
