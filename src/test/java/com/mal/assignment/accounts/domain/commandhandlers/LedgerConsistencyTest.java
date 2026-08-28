package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LedgerConsistencyTest {

    @Test
    void closingBalanceMatchesOpeningPlusSignedAmountsThroughThatValueDay() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        module.commandBus().dispatch(new BookDebitCommand("E2", "ACC-001", 1, 95_000L, "E2"));
        module.commandBus().dispatch(new BookCreditCommand("E4", "ACC-001", 3, 40_000L, "E4"));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(25_000L, account.closingBalance(1));
        assertEquals(25_000L, account.closingBalance(2));
        assertEquals(65_000L, account.closingBalance(3));
        long summed = account.openingBalanceInMinorUnits();
        for (LedgerEntry entry : account.ledgerEntriesSortedByValueDay()) {
            if (entry.valueDay() <= 3) {
                summed += entry.signedAmountInMinorUnits();
            }
        }
        assertEquals(summed, account.closingBalance(3));
    }

    @Test
    void rowBalancesAreStaleOnceAnotherEntrySharesOrPrecedesTheValueDay() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        module.commandBus().dispatch(new BookDebitCommand("E2", "ACC-001", 1, 95_000L, "E2"));
        Account account = module.account("ACC-001").orElseThrow();
        LedgerEntry firstOfDay1 = account.entries().stream()
                .filter(entry -> "E1".equals(entry.idempotencyKey()))
                .findFirst()
                .orElseThrow();
        LedgerEntry lastOfDay1 = account.entries().stream()
                .filter(entry -> "E2".equals(entry.idempotencyKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(120_000L, firstOfDay1.balanceAfterInMinorUnits());
        assertEquals(25_000L, lastOfDay1.balanceAfterInMinorUnits());
        assertEquals(25_000L, account.closingBalance(1));

        module.commandBus().dispatch(new BookCreditCommand("E4", "ACC-001", 3, 40_000L, "E4"));
        module.commandBus().dispatch(new BookDebitCommand("E7", "ACC-001", 2, 62_000L, "E7"));
        account = module.account("ACC-001").orElseThrow();
        LedgerEntry backdated = account.entryByReference("E7").orElseThrow();
        assertEquals(3_000L, backdated.balanceAfterInMinorUnits());
        assertEquals(-37_000L, account.closingBalance(2));
        LedgerEntry day3 = account.entries().stream()
                .filter(entry -> "E4".equals(entry.idempotencyKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(65_000L, day3.balanceAfterInMinorUnits());
        assertEquals(3_000L, account.closingBalance(3));
    }
}
