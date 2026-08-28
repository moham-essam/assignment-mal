package com.mal.assignment.accounts.domain.commands;

public record SettleAuthorizationCommand(
        String commandId,
        String accountId,
        String authorizationReference,
        int valueDay,
        long amountInMinorUnits
) implements Command {
}
