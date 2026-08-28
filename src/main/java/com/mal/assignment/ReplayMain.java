package com.mal.assignment;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.reports.ReportBuilder;
import com.mal.assignment.accounts.infrastructure.config.AccountSeedConfig;
import com.mal.assignment.accounts.infrastructure.reports.ReportPrinter;
import com.mal.assignment.simulation.ScenarioFixture;
import com.mal.assignment.simulation.SimulationEngine;
import com.mal.assignment.simulation.SimulationResult;

import java.util.ArrayList;
import java.util.List;

public final class ReplayMain {

    private ReplayMain() {
    }

    public static void main(String[] args) {
        AccountsModule module = AccountsModule.shipped();
        ReportBuilder reportBuilder = new ReportBuilder();
        ReportPrinter printer = new ReportPrinter();
        System.out.print(printer.printHeader());
        SimulationResult result = new SimulationEngine(module.commandBus(), module.ledgerConfig())
                .run(ScenarioFixture.assignment(), day ->
                        System.out.print(printer.printDay(reportBuilder.buildDay(accounts(module), day))));
        System.out.print(printer.printSummary(
                reportBuilder.build(accounts(module), module.ledgerConfig().windowDays(), result)
        ));
    }

    private static List<Account> accounts(AccountsModule module) {
        List<Account> accounts = new ArrayList<>();
        for (AccountSeedConfig seed : module.ledgerConfig().accounts()) {
            accounts.add(module.account(seed.id()).orElseThrow());
        }
        return accounts;
    }
}
