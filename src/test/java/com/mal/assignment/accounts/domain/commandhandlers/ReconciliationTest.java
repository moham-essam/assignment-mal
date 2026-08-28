package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.commands.ReconcileFromDayCommand;
import com.mal.assignment.accounts.domain.commands.ReverseTransactionCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.support.SeedLedger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationTest {

    @Test
    void loadsValueDayOrderComputesLocalRunningAndDoesNotRewriteExistingBalances() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        Account account = module.account("ACC-001").orElseThrow();
        LedgerEntry credit = account.entries().iterator().next();
        long before = credit.balanceBeforeInMinorUnits();
        long after = credit.balanceAfterInMinorUnits();
        SeedLedger.append(module, "ACC-001", LedgerEntry.draft("E7", 2, -62_000L, LedgerEntryType.DEBIT, "E7"));
        module.commandBus().dispatch(new ReconcileFromDayCommand("recon", "ACC-001", 1, 2));
        account = module.account("ACC-001").orElseThrow();
        LedgerEntry sameCredit = account.entries().stream()
                .filter(entry -> "E1".equals(entry.idempotencyKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(before, sameCredit.balanceBeforeInMinorUnits());
        assertEquals(after, sameCredit.balanceAfterInMinorUnits());
        assertEquals(-2500L, account.netFeeSignedAmount(2));
        assertEquals(0L, account.accruedDailyInterestByDay().get(2));
        assertEquals(10L, account.accruedDailyInterestByDay().get(1));
    }

    @Test
    void secondReconcileIsIdempotentOnFees() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        SeedLedger.append(module, "ACC-001", LedgerEntry.draft("E7", 2, -62_000L, LedgerEntryType.DEBIT, "E7"));
        module.commandBus().dispatch(new ReconcileFromDayCommand("recon-1", "ACC-001", 2, 2));
        Account account = module.account("ACC-001").orElseThrow();
        int feeRows = feeRowCount(account);
        module.commandBus().dispatch(new ReconcileFromDayCommand("recon-2", "ACC-001", 2, 2));
        assertEquals(feeRows, feeRowCount(module.account("ACC-001").orElseThrow()));
    }

    @Test
    void reverseDispatchesReconcileWhichReversesFeesOnFollowingDays() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        SeedLedger.append(module, "ACC-001", LedgerEntry.draft("E7", 2, -62_000L, LedgerEntryType.DEBIT, "E7"));
        module.commandBus().dispatch(new EndOfDayCommand("eod2", "ACC-001", 2));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(-2500L, account.netFeeSignedAmount(2));
        module.commandBus().dispatch(new ReverseTransactionCommand("E9", "ACC-001", "E7"));
        account = module.account("ACC-001").orElseThrow();
        assertEquals(0L, account.netFeeSignedAmount(2));
        assertTrue(account.entries().stream().anyMatch(entry -> entry.type() == LedgerEntryType.FEE_REVERSAL));
        assertEquals(10L, account.accruedDailyInterestByDay().get(1));
        assertTrue(account.accruedDailyInterestByDay().get(2) >= 0L);
    }

    @Test
    void reconFromLaterDayStartsAtPreviousClosingIncludingFees() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        SeedLedger.append(module, "ACC-001", LedgerEntry.draft("D1", 1, -26_000L, LedgerEntryType.DEBIT, "D1"));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        Account afterDay1 = module.account("ACC-001").orElseThrow();
        assertEquals(-3_500L, afterDay1.closingBalance(1));
        SeedLedger.append(module, "ACC-001", LedgerEntry.draft("C2", 2, 2_000L, LedgerEntryType.CREDIT, "C2"));
        module.commandBus().dispatch(new ReconcileFromDayCommand("recon", "ACC-001", 2, 2));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(-2_500L, account.netFeeSignedAmount(2));
        assertEquals(-3_500L + 2_000L - 2_500L, account.amountInMinorUnits());
        assertEquals(0L, account.accruedDailyInterestByDay().get(2));
    }

    private static int feeRowCount(Account account) {
        return (int) account.entries().stream()
                .filter(entry -> entry.type().ownDayFee())
                .count();
    }
}
