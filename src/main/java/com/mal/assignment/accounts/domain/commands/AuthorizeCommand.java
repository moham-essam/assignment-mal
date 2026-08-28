package com.mal.assignment.accounts.domain.commands;

public record AuthorizeCommand(
        String commandId,
        String accountId,
        String authorizationReference,
        long requestedAmountInMinorUnits
) implements Command {
}
