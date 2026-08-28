package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.AuthorizationStatus;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import com.mal.assignment.accounts.domain.services.AuthorizationService;
import com.mal.assignment.support.StubUnitOfWork;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationTest {

    @Test
    void approvesWhenAvailableCoversTheHold() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 25_000L, "E1"));
        CommandResult result = module.commandBus().dispatch(
                new AuthorizeCommand("E3", "ACC-001", "Auth-A", 20_000L));
        assertTrue(result.error().isEmpty());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(AuthorizationStatus.APPROVED, account.authorizations().iterator().next().status());
        assertEquals(20_000L, account.holdAmountInMinorUnits());
        assertEquals(5_000L, account.availableBalanceInMinorUnits());
    }

    @Test
    void declinesAndCommitsWhenAvailableIncludingHoldsIsTooLow() {
        AccountsModule module = AccountsModule.shipped();
        module.commandBus().dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L));
        module.commandBus().dispatch(new BookCreditCommand("E1", "ACC-001", 1, 46_500L, "E1"));
        CommandResult result = module.commandBus().dispatch(
                new AuthorizeCommand("E8", "ACC-001", "Auth-B", 50_000L));
        assertTrue(result.failed());
        assertEquals(FailureReason.INSUFFICIENT_AVAILABLE_BALANCE, result.error().orElseThrow().reason());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(AuthorizationStatus.DECLINED, account.authorizations().iterator().next().status());
        assertEquals(0L, account.holdAmountInMinorUnits());
    }

    @Test
    void commitFailureIsFailClosed() {
        Account account = new Account("ACC-001", Currency.AED, 25_000L);
        StubUnitOfWork stub = new StubUnitOfWork(
                account,
                LedgerDomainException.of(FailureReason.CONCURRENT_MODIFICATION, "commit failed")
        );
        AuthorizationService authorizationService = new AuthorizationService(stub);
        AuthorizeCommandHandler handler = new AuthorizeCommandHandler(authorizationService);
        CommandResult result = handler.handle(new AuthorizeCommand("E3", "ACC-001", "Auth-A", 20_000L));
        assertTrue(result.failed());
        assertEquals(FailureReason.CONCURRENT_MODIFICATION, result.error().orElseThrow().reason());
    }
}
