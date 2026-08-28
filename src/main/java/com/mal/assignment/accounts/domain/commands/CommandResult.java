package com.mal.assignment.accounts.domain.commands;

import com.mal.assignment.accounts.domain.models.LedgerError;

import java.util.Optional;

public record CommandResult(
        long version,
        Optional<LedgerError> error
) {

    public static CommandResult ok(long version) {
        return new CommandResult(version, Optional.empty());
    }

    public static CommandResult failure(LedgerError error) {
        return new CommandResult(0L, Optional.of(error));
    }

    public boolean failed() {
        return error.isPresent();
    }
}
