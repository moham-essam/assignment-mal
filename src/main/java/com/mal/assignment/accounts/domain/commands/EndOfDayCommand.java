package com.mal.assignment.accounts.domain.commands;

public record EndOfDayCommand(
        String commandId,
        String accountId,
        int day
) implements Command {
}
