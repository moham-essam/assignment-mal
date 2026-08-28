package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;

import java.util.function.LongSupplier;

final class HandlerSupport {

    private HandlerSupport() {
    }

    static CommandResult from(LongSupplier action) {
        try {
            return CommandResult.ok(action.getAsLong());
        } catch (LedgerDomainException exception) {
            return CommandResult.failure(exception.error());
        }
    }
}
