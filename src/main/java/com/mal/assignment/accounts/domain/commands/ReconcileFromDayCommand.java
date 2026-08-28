package com.mal.assignment.accounts.domain.commands;

public record ReconcileFromDayCommand(
        String commandId,
        String accountId,
        int fromDay,
        int toDay
) implements Command {
}
