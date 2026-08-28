package com.mal.assignment.accounts.infrastructure.config;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public record LedgerConfig(
        int windowDays,
        RoundingMode roundingMode,
        Map<String, CurrencyScaleConfig> currencies,
        InterestConfig interest,
        OverdraftConfig overdraft,
        List<AccountSeedConfig> accounts
) {
}
