package com.mal.assignment.accounts.infrastructure.config;

public record InterestConfig(
        int dailyRateNumerator,
        int dailyRateDenominator,
        int capitalizationDay
) {
}
