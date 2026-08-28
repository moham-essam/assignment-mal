package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
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
    void debitMayOverdrawTheBookedAmount() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 46_500L, "E1"));
        CommandResult result = module.commandBus().dispatch(
                new BookDebitCommand("E7", "ACC-001", 2, 62_000L, "E7"));
        assertFalse(result.failed());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(-15_500L, account.amountInMinorUnits());
        assertTrue(account.entryByReference("E7").isPresent());
    }

    @Test
    void debitEqualToAvailableSucceeds() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        CommandResult result = module.commandBus().dispatch(
                new BookDebitCommand("exact", "ACC-001", 1, 25_000L, "exact"));
        assertFalse(result.failed());
        assertEquals(0L, module.account("ACC-001").orElseThrow().amountInMinorUnits());
    }

    @Test
    void holdsDoNotBlockDebit() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new AuthorizeCommand("E3", "ACC-001", "Auth-A", 20_000L));
        CommandResult result = module.commandBus().dispatch(
                new BookDebitCommand("over", "ACC-001", 1, 6_000L, "over"));
        assertFalse(result.failed());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(19_000L, account.amountInMinorUnits());
        assertEquals(-1_000L, account.availableBalanceInMinorUnits());
    }

    @Test
    void backdatedDebitReconcilesClosedDays() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        CommandResult result = module.commandBus().dispatch(
                new BookDebitCommand("E7", "ACC-001", 1, 30_000L, "E7"));
        assertFalse(result.failed());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(-5_000L - 2_500L, account.amountInMinorUnits());
        assertEquals(-2_500L, account.netFeeSignedAmount(1));
        assertEquals(0L, account.accruedDailyInterestByDay().get(1));
    }

    @Test
    void sameDayDebitDoesNotReconcileBeforeClose() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new BookDebitCommand("E7", "ACC-001", 1, 30_000L, "E7"));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(-5_000L, account.amountInMinorUnits());
        assertTrue(account.accruedDailyInterestByDay().isEmpty());
        assertTrue(account.entries().stream().noneMatch(entry -> entry.type().ownDayFee()));
    }
}
