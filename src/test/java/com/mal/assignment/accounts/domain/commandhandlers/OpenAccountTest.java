package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenAccountTest {

    @Test
    void openPersistsCurrencyAndOpeningBalance() {
        AccountsModule module = AccountsModule.shipped();
        CommandResult result = module.commandBus().dispatch(
                new OpenAccountCommand("open", "ACC-001", Currency.AED, 1_200L));
        assertFalse(result.failed());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(Currency.AED, account.currency());
        assertEquals(1_200L, account.openingBalanceInMinorUnits());
        assertEquals(1_200L, account.amountInMinorUnits());
    }

    @Test
    void secondOpenKeepsTheFirstOpeningBalance() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open-1", "ACC-001", Currency.AED, 1_200L));
        module.commandBus().dispatch(new OpenAccountCommand("open-2", "ACC-001", Currency.AED, 99L));
        assertEquals(1_200L, module.account("ACC-001").orElseThrow().openingBalanceInMinorUnits());
    }
}
