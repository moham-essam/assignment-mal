package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookCreditTest {

    @Test
    void committedCreditRowHasBalanceBeforeAndAfter() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        CommandResult result = module.commandBus().dispatch(
                new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        assertFalse(result.failed());
        Account account = module.account("ACC-001").orElseThrow();
        LedgerEntry row = account.entries().iterator().next();
        assertEquals(0L, row.balanceBeforeInMinorUnits());
        assertEquals(120_000L, row.balanceAfterInMinorUnits());
        assertEquals(120_000L, account.amountInMinorUnits());
    }

    @Test
    void unknownAccountIsADomainFailure() {
        AccountsModule module = AccountsModule.shipped();
        CommandResult result = module.commandBus().dispatch(
                new BookCreditCommand("E1", "MISSING", 1, 100L, "E1"));
        assertTrue(result.failed());
        assertEquals(FailureReason.UNKNOWN_ACCOUNT, result.error().orElseThrow().reason());
    }
}
