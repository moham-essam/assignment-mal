package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.EndOfDayBatchCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.support.SeedLedger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndOfDayTest {

    @Test
    void positiveClosingAccruesTruncatedInterestAndDoesNotBookAFee() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(10L, account.accruedDailyInterestByDay().get(1));
        assertTrue(account.entries().stream().noneMatch(entry -> entry.type() == LedgerEntryType.OVERDRAFT_FEE));
    }

    @Test
    void negativeClosingBooksOverdraftFeeAndAccruesZero() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        SeedLedger.append(module, "ACC-001", LedgerEntry.draft("E7", 2, -62_000L, LedgerEntryType.DEBIT, "E7"));
        module.commandBus().dispatch(new EndOfDayCommand("eod2", "ACC-001", 2));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(0L, account.accruedDailyInterestByDay().get(2));
        assertEquals(-2500L, account.netFeeSignedAmount(2));
        assertEquals(-62_000L, account.postingsOnlyClosing(2));
    }

    @Test
    void batchClosesEveryOpenedAccountNotOnlyConfiguredSeeds() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-003", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("c1", "ACC-003", 1, 25_000L, "c1"));
        CommandResult result = module.commandBus().dispatch(new EndOfDayBatchCommand("eod-batch", 1));
        assertFalse(result.failed());
        Account account = module.account("ACC-003").orElseThrow();
        assertEquals(10L, account.accruedDailyInterestByDay().get(1));
    }

    @Test
    void zeroClosingAccruesZeroAndDoesNotBookAFee() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(0L, account.accruedDailyInterestByDay().get(1));
        assertTrue(account.entries().stream().noneMatch(entry -> entry.type() == LedgerEntryType.OVERDRAFT_FEE));
    }

    @Test
    void unknownAccountIsADomainFailure() {
        AccountsModule module = AccountsModule.shipped();
        CommandResult result = module.commandBus().dispatch(new EndOfDayCommand("eod1", "MISSING", 1));
        assertTrue(result.failed());
        assertEquals(FailureReason.UNKNOWN_ACCOUNT, result.error().orElseThrow().reason());
    }
}
