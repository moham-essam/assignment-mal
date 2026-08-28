package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookDebitTest {

    @Test
    void committedDebitRowHasBalanceBeforeAndAfter() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        CommandResult result = module.commandBus().dispatch(
                new BookDebitCommand("E2", "ACC-001", 1, 95_000L, "E2"));
        assertFalse(result.failed());
        Account account = module.account("ACC-001").orElseThrow();
        LedgerEntry debit = account.entries().stream()
                .filter(entry -> entry.type() == LedgerEntryType.DEBIT)
                .findFirst()
                .orElseThrow();
        assertEquals(120_000L, debit.balanceBeforeInMinorUnits());
        assertEquals(25_000L, debit.balanceAfterInMinorUnits());
        assertEquals(25_000L, account.amountInMinorUnits());
    }

    @Test
    void insufficientAvailableDoesNotAppendOrCommitARow() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 46_500L, "E1"));
        CommandResult result = module.commandBus().dispatch(
                new BookDebitCommand("E7", "ACC-001", 2, 62_000L, "E7"));
        assertTrue(result.failed());
        assertEquals(FailureReason.INSUFFICIENT_AVAILABLE_BALANCE, result.error().orElseThrow().reason());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(1, account.entries().size());
        assertEquals(46_500L, account.amountInMinorUnits());
    }
}
