package com.mal.assignment.accounts.infrastructure.reports;

import com.mal.assignment.accounts.domain.models.LedgerError;
import com.mal.assignment.accounts.domain.models.MinorUnits;
import com.mal.assignment.accounts.domain.reports.AccountReport;
import com.mal.assignment.accounts.domain.reports.DailyReport;
import com.mal.assignment.accounts.domain.reports.DailySnapshot;
import com.mal.assignment.accounts.domain.reports.ScenarioReport;

public final class ReportPrinter {

    public String print(ScenarioReport report) {
        StringBuilder out = new StringBuilder();
        out.append(printHeader());
        for (DailyReport day : report.days()) {
            out.append(printDay(day));
        }
        out.append(printSummary(report));
        return out.toString();
    }

    public String printHeader() {
        return "Assignment ledger replay\n========================\n";
    }

    public String printDay(DailyReport day) {
        StringBuilder out = new StringBuilder();
        out.append('\n').append("Day ").append(day.day()).append('\n');
        for (DailySnapshot snapshot : day.snapshots()) {
            out.append("  ")
                    .append(snapshot.accountId())
                    .append("  closing ")
                    .append(MinorUnits.format(snapshot.closingInMinorUnits(), snapshot.currency()))
                    .append("  interest ")
                    .append(MinorUnits.format(snapshot.accruedInterestInMinorUnits(), snapshot.currency()))
                    .append("  fees ")
                    .append(MinorUnits.format(snapshot.netFeeInMinorUnits(), snapshot.currency()))
                    .append("  holds ")
                    .append(MinorUnits.format(snapshot.holdInMinorUnits(), snapshot.currency()))
                    .append('\n');
        }
        return out.toString();
    }

    public String printSummary(ScenarioReport report) {
        StringBuilder out = new StringBuilder();
        out.append('\n').append("Accounts\n");
        for (AccountReport account : report.accounts()) {
            out.append("  ")
                    .append(account.accountId())
                    .append("  booked ")
                    .append(MinorUnits.format(account.amountInMinorUnits(), account.currency()))
                    .append("  capitalized ")
                    .append(MinorUnits.format(account.capitalizedInterestInMinorUnits(), account.currency()))
                    .append('\n');
        }
        out.append('\n').append("Command failures\n");
        if (report.errors().isEmpty()) {
            out.append("  none\n");
        } else {
            for (LedgerError error : report.errors()) {
                out.append("  ")
                        .append(error.reason())
                        .append("  ")
                        .append(error.accountId() == null ? "" : error.accountId() + "  ")
                        .append(error.message())
                        .append('\n');
            }
        }
        return out.toString();
    }
}
