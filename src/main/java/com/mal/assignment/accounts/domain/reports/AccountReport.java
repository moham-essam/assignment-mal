package com.mal.assignment.accounts.domain.reports;

import com.mal.assignment.accounts.domain.models.Currency;

public record AccountReport(
        String accountId,
        Currency currency,
        long capitalizedInterestInMinorUnits,
        long amountInMinorUnits
) {
}
