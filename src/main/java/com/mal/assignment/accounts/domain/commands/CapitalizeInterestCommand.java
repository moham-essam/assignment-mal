package com.mal.assignment.accounts.domain.commands;

public record CapitalizeInterestCommand(
        String commandId,
        String accountId,
        int valueDay
) implements Command {
}
