package com.mal.assignment.accounts.domain.reports;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.commands.SettleAuthorizationCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.infrastructure.reports.ReportPrinter;
import com.mal.assignment.simulation.SimulationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportBuilderTest {

    @Test
    void dailySnapshotUsesClosingInterestAndFees() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        Account account = module.account("ACC-001").orElseThrow();
        ScenarioReport report = new ReportBuilder().build(
                List.of(account),
                1,
                new SimulationResult(List.of(), List.of(), List.of())
        );
        DailySnapshot snapshot = report.days().getFirst().snapshots().getFirst();
        assertEquals("ACC-001", snapshot.accountId());
        assertEquals(25_000L, snapshot.closingInMinorUnits());
        assertEquals(0L, snapshot.accruedInterestInMinorUnits());
        assertEquals(0L, snapshot.netFeeInMinorUnits());
        assertEquals(0L, snapshot.holdInMinorUnits());
        assertEquals(25_000L, report.accounts().getFirst().amountInMinorUnits());
    }

    @Test
    void dailySnapshotReconstructsHoldThroughSettlementDay() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new EndOfDayCommand("EOD-1", "ACC-001", 1));
        module.commandBus().dispatch(new AuthorizeCommand("E3", "ACC-001", "Auth-A", 20_000L));
        module.commandBus().dispatch(new SettleAuthorizationCommand("E5", "ACC-001", "Auth-A", 4, 18_500L));
        Account account = module.account("ACC-001").orElseThrow();
        ScenarioReport report = new ReportBuilder().build(
                List.of(account),
                4,
                new SimulationResult(List.of(), List.of(), List.of())
        );
        assertEquals(0L, report.days().get(0).snapshots().getFirst().holdInMinorUnits());
        assertEquals(20_000L, report.days().get(1).snapshots().getFirst().holdInMinorUnits());
        assertEquals(20_000L, report.days().get(2).snapshots().getFirst().holdInMinorUnits());
        assertEquals(0L, report.days().get(3).snapshots().getFirst().holdInMinorUnits());
    }

    @Test
    void printerIncludesFormattedClosings() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        ScenarioReport report = new ReportBuilder().build(
                List.of(module.account("ACC-001").orElseThrow()),
                1,
                new SimulationResult(List.of(), List.of(), List.of())
        );
        String printed = new ReportPrinter().print(report);
        assertTrue(printed.contains("Day 1"));
        assertTrue(printed.contains("250.00"));
        assertTrue(printed.contains("holds"));
        assertTrue(printed.contains("none"));
    }
}
