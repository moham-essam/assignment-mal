package com.mal.assignment.accounts.domain.models;

import java.util.Locale;

public enum Currency {
    AED(2),
    BHD(3);

    private final int scale;

    Currency(int scale) {
        this.scale = scale;
    }

    public int scale() {
        return scale;
    }

    public static Currency fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw LedgerDomainException.of(FailureReason.UNKNOWN_CURRENCY, "currency code is required");
        }
        try {
            return Currency.valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw LedgerDomainException.of(FailureReason.UNKNOWN_CURRENCY, "Unknown currency " + code);
        }
    }
}
