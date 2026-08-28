package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.CapitalizeInterestCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapitalizationTest {

    @Test
    void capitalizationEqualsTheSumOfDailyAccruals() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new EndOfDayCommand("eod1", "ACC-001", 1));
        module.commandBus().dispatch(new EndOfDayCommand("eod2", "ACC-001", 2));
        Account account = module.account("ACC-001").orElseThrow();
        long expected = account.summedAccruals();
        module.commandBus().dispatch(new CapitalizeInterestCommand("cap", "ACC-001", 6));
        account = module.account("ACC-001").orElseThrow();
        assertEquals(expected, account.capitalizedInterestInMinorUnits());
        long capAmount = account.entries().stream()
                .filter(entry -> entry.type() == LedgerEntryType.INTEREST_CAPITALIZATION)
                .mapToLong(entry -> entry.signedAmountInMinorUnits())
                .sum();
        assertEquals(expected, capAmount);
        assertEquals(25_000L + expected, account.closingBalance(6));
        assertEquals(25_000L, account.closingBalance(5));
    }

    @Test
    void zeroAccrualsDoNotAppendACapitalizationRow() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new CapitalizeInterestCommand("cap", "ACC-001", 6));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(0L, account.capitalizedInterestInMinorUnits());
        assertTrue(account.entries().isEmpty());
    }
}
