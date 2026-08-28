package com.mal.assignment.simulation;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.models.LedgerError;

import java.util.List;

public record SimulationResult(
        List<CommandResult> commandResults,
        List<String> dispatchedCommandIds,
        List<Checkpoint> checkpoints
) {

    public List<LedgerError> errors() {
        return commandResults.stream()
                .flatMap(result -> result.error().stream())
                .toList();
    }
}
