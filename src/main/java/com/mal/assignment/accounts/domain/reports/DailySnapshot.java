package com.mal.assignment.accounts.domain.reports;

import com.mal.assignment.accounts.domain.models.Currency;

public record DailySnapshot(
        String accountId,
        Currency currency,
        int day,
        long closingInMinorUnits,
        long accruedInterestInMinorUnits,
        long netFeeInMinorUnits,
        long holdInMinorUnits
) {
}
