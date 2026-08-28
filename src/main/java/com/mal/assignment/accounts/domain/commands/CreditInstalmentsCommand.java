package com.mal.assignment.accounts.domain.commands;

public record CreditInstalmentsCommand(
        String commandId,
        String accountId,
        int valueDay,
        long totalAmountInMinorUnits,
        int parts
) implements Command {
}
