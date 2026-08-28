package com.mal.assignment.accounts.infrastructure.config;

public record AccountSeedConfig(
        String id,
        String currency,
        long openingBalanceInMinorUnits
) {
}
