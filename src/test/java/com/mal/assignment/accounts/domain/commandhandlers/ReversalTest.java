package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.commands.ReverseTransactionCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReversalTest {

    @Test
    void reversalKeepsOriginalValueDayAndRestoresAmount() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        module.commandBus().dispatch(new BookDebitCommand("E2", "ACC-001", 1, 95_000L, "E2"));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        CommandResult result = module.commandBus().dispatch(
                new ReverseTransactionCommand("E9", "ACC-001", "E2"));
        assertTrue(result.error().isEmpty());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(120_000L, account.amountInMinorUnits());
        assertTrue(account.entries().stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.REVERSAL && entry.valueDay() == 1));
    }

    @Test
    void missingOriginalIsNotReversible() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        CommandResult result = module.commandBus().dispatch(
                new ReverseTransactionCommand("E9", "ACC-001", "E7"));
        assertTrue(result.failed());
        assertEquals(FailureReason.NOT_REVERSIBLE, result.error().orElseThrow().reason());
    }

    @Test
    void secondReverseIsAlreadyReversed() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        module.commandBus().dispatch(new ReverseTransactionCommand("first", "ACC-001", "E1"));
        CommandResult result = module.commandBus().dispatch(
                new ReverseTransactionCommand("second", "ACC-001", "E1"));
        assertTrue(result.failed());
        assertEquals(FailureReason.ALREADY_REVERSED, result.error().orElseThrow().reason());
    }

    @Test
    void reverseBeforeAnyClosedDayDoesNotDispatchReconcile() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        module.commandBus().dispatch(new ReverseTransactionCommand("E9", "ACC-001", "E1"));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(0L, account.amountInMinorUnits());
        assertTrue(account.accruedDailyInterestByDay().isEmpty());
        assertTrue(account.entries().stream().noneMatch(entry -> entry.type().ownDayFee()));
    }
}
