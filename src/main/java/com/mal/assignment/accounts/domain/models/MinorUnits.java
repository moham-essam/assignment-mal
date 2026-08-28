package com.mal.assignment.accounts.domain.models;

public final class MinorUnits {

    private MinorUnits() {
    }

    public static long[] splitEvenly(long totalInMinorUnits, int parts) {
        if (parts <= 0) {
            throw LedgerDomainException.of(
                    FailureReason.INVALID_SPLIT_PARTS,
                    "parts must be positive"
            );
        }
        long base = totalInMinorUnits / parts;
        long remainder = totalInMinorUnits % parts;
        long[] split = new long[parts];
        for (int i = 0; i < parts; i++) {
            split[i] = base + (i < remainder ? 1 : 0);
        }
        return split;
    }

    public static String format(long amountInMinorUnits, Currency currency) {
        if (currency == null) {
            throw LedgerDomainException.of(FailureReason.UNKNOWN_CURRENCY, "currency is required");
        }
        int scale = currency.scale();
        long absolute = Math.abs(amountInMinorUnits);
        long major = absolute / pow10(scale);
        long minor = absolute % pow10(scale);
        String sign = amountInMinorUnits < 0 ? "-" : "";
        return sign + major + "." + pad(minor, scale);
    }

    private static long pow10(int scale) {
        long value = 1L;
        for (int i = 0; i < scale; i++) {
            value *= 10L;
        }
        return value;
    }

    private static String pad(long minor, int scale) {
        String raw = Long.toString(minor);
        if (raw.length() >= scale) {
            return raw;
        }
        return "0".repeat(scale - raw.length()) + raw;
    }
}
