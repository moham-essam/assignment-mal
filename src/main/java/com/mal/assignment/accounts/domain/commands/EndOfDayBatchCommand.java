package com.mal.assignment.accounts.domain.commands;

public record EndOfDayBatchCommand(
        String commandId,
        int day
) implements Command {
}
