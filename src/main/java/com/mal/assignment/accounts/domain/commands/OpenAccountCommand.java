package com.mal.assignment.accounts.domain.commands;

import com.mal.assignment.accounts.domain.models.Currency;

public record OpenAccountCommand(
        String commandId,
        String accountId,
        Currency currency,
        long openingBalanceInMinorUnits
) implements Command {
}
