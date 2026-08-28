package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.infrastructure.config.LedgerConfig;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public final class LedgerPolicies {

    private final LedgerConfig config;
    private final OverdraftPolicy overdraftPolicy;
    private final InterestPolicy interestPolicy;

    public LedgerPolicies(LedgerConfig config) {
        this.config = config;
        Map<Currency, Long> fees = new LinkedHashMap<>();
        config.overdraft().feesInMinorUnits().forEach((code, fee) ->
                fees.put(Currency.fromCode(code), fee));
        this.overdraftPolicy = new OverdraftPolicy(fees);
        this.interestPolicy = new InterestPolicy(
                config.interest().dailyRateNumerator(),
                config.interest().dailyRateDenominator(),
                config.roundingMode()
        );
    }
}
