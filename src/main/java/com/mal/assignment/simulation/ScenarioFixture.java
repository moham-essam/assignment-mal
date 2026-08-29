package com.mal.assignment.simulation;

import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.CreditInstalmentsCommand;
import com.mal.assignment.accounts.domain.commands.ReverseTransactionCommand;
import com.mal.assignment.accounts.domain.commands.SettleAuthorizationCommand;

import java.util.List;

public final class ScenarioFixture {

    private ScenarioFixture() {
    }

    public static List<ScheduledCommand> assignment() {
        return List.of(
                new ScheduledCommand(1, new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1")),
                new ScheduledCommand(1, new BookDebitCommand("E2", "ACC-001", 1, 95_000L, "E2")),
                new ScheduledCommand(2, new AuthorizeCommand("E3", "ACC-001", "Auth-A", 20_000L)),
                new ScheduledCommand(3, new BookCreditCommand("E4", "ACC-001", 3, 40_000L, "E4")),
                new ScheduledCommand(4, new SettleAuthorizationCommand("E5", "ACC-001", "Auth-A", 4, 18_500L)),
                new ScheduledCommand(4, new SettleAuthorizationCommand("E6", "ACC-001", "Auth-Z", 4, 18_000L)),
                new ScheduledCommand(5, new BookDebitCommand("E7", "ACC-001", 2, 62_000L, "E7")),
                new ScheduledCommand(5, new AuthorizeCommand("E8", "ACC-001", "Auth-B", 90_00L)),
                new ScheduledCommand(5, new CreditInstalmentsCommand("E10", "ACC-002", 5, 10_000L, 3)),
                new ScheduledCommand(6, new ReverseTransactionCommand("E9", "ACC-001", "E7"))
        );
    }
}
