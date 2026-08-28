package com.mal.assignment.accounts.domain.reports;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.simulation.SimulationResult;

import java.util.ArrayList;
import java.util.List;

public final class ReportBuilder {

    public DailyReport buildDay(List<Account> accounts, int day) {
        List<DailySnapshot> snapshots = new ArrayList<>();
        for (Account account : accounts) {
            snapshots.add(new DailySnapshot(
                    account.id(),
                    account.currency(),
                    day,
                    account.closingBalance(day),
                    account.accruedDailyInterestByDay().getOrDefault(day, 0L),
                    account.netFeeSignedAmount(day),
                    account.holdAmountInMinorUnits(day)
            ));
        }
        return new DailyReport(day, List.copyOf(snapshots));
    }

    public ScenarioReport build(List<Account> accounts, int windowDays, SimulationResult result) {
        List<DailyReport> days = new ArrayList<>();
        for (int day = 1; day <= windowDays; day++) {
            days.add(buildDay(accounts, day));
        }
        List<AccountReport> accountReports = new ArrayList<>();
        for (Account account : accounts) {
            accountReports.add(new AccountReport(
                    account.id(),
                    account.currency(),
                    account.capitalizedInterestInMinorUnits(),
                    account.amountInMinorUnits()
            ));
        }
        return new ScenarioReport(List.copyOf(days), List.copyOf(accountReports), result.errors());
    }
}
