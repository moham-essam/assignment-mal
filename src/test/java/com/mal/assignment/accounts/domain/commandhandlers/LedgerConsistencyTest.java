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
}
