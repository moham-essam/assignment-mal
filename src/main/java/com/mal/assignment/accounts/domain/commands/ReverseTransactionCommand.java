package com.mal.assignment.accounts.domain.commands;

public record ReverseTransactionCommand(
        String commandId,
        String accountId,
        String originalReferenceId
) implements Command {
}
