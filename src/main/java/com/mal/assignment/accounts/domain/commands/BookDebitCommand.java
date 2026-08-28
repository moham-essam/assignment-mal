package com.mal.assignment.accounts.domain.commands;

public record BookDebitCommand(
        String commandId,
        String accountId,
        int valueDay,
        long amountInMinorUnits,
        String referenceId
) implements Command {
}
