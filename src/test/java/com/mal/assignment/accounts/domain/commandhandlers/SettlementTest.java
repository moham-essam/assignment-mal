package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.commands.SettleAuthorizationCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementTest {

    @Test
    void booksSettlementAndReleasesTheFullHold() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        module.commandBus().dispatch(new AuthorizeCommand("E3", "ACC-001", "Auth-A", 20_000L));
        CommandResult result = module.commandBus().dispatch(
                new SettleAuthorizationCommand("E5", "ACC-001", "Auth-A", 4, 18_500L));
        assertTrue(result.error().isEmpty());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(0L, account.holdAmountInMinorUnits());
        assertEquals(6_500L, account.amountInMinorUnits());
        assertTrue(account.entries().stream().anyMatch(entry -> entry.type() == LedgerEntryType.SETTLEMENT));
    }

    @Test
    void unknownAuthorizationIsRejected() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        CommandResult result = module.commandBus().dispatch(
                new SettleAuthorizationCommand("E6", "ACC-001", "Auth-Z", 4, 18_500L));
        assertTrue(result.failed());
        assertEquals(FailureReason.UNKNOWN_AUTHORIZATION, result.error().orElseThrow().reason());
        assertTrue(module.account("ACC-001").orElseThrow().entries().isEmpty());
    }

    @Test
    void declinedAuthorizationCannotBeSettled() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 10_000L, "E1"));
        module.commandBus().dispatch(new AuthorizeCommand("E8", "ACC-001", "Auth-B", 50_000L));
        CommandResult result = module.commandBus().dispatch(
                new SettleAuthorizationCommand("E6", "ACC-001", "Auth-B", 4, 18_500L));
        assertTrue(result.failed());
        assertEquals(FailureReason.UNKNOWN_AUTHORIZATION, result.error().orElseThrow().reason());
        assertEquals(10_000L, module.account("ACC-001").orElseThrow().amountInMinorUnits());
    }
}
