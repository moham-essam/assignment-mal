package com.mal.assignment.simulation;

import com.mal.assignment.accounts.domain.commands.Command;

public record ScheduledCommand(int arrivalDay, Command command) {
}
