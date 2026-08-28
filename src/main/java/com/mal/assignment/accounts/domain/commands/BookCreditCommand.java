package com.mal.assignment.accounts.domain.commands;

public record BookCreditCommand(
        String commandId,
        String accountId,
        int valueDay,
        long amountInMinorUnits,
        String referenceId
) implements Command {
}
