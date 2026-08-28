package com.mal.assignment.accounts.domain.reports;

import com.mal.assignment.accounts.domain.models.LedgerError;

import java.util.List;

public record ScenarioReport(
        List<DailyReport> days,
        List<AccountReport> accounts,
        List<LedgerError> errors
) {
}
