package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.CreditInstalmentsCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditInstalmentsTest {

    @Test
    void splitsTenBhdInto3334Then3333Then3333OnValueDayFive() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-002", Currency.BHD, 0L));
        module.commandBus().dispatch(new CreditInstalmentsCommand("E10", "ACC-002", 5, 10_000L, 3));
        Account account = module.account("ACC-002").orElseThrow();
        List<LedgerEntry> rows = account.ledgerEntriesSortedByValueDay();
        assertEquals(3, rows.size());
        assertEquals(3334L, rows.get(0).signedAmountInMinorUnits());
        assertEquals(3333L, rows.get(1).signedAmountInMinorUnits());
        assertEquals(3333L, rows.get(2).signedAmountInMinorUnits());
        assertEquals(10_000L, rows.stream().mapToLong(LedgerEntry::signedAmountInMinorUnits).sum());
        rows.forEach(row -> assertEquals(5, row.valueDay()));
    }

    @Test
    void unknownAccountIsADomainFailure() {
        AccountsModule module = AccountsModule.shipped();
        CommandResult result = module.commandBus().dispatch(
                new CreditInstalmentsCommand("E10", "MISSING", 5, 10_000L, 3));
        assertTrue(result.failed());
        assertEquals(FailureReason.UNKNOWN_ACCOUNT, result.error().orElseThrow().reason());
    }
}
