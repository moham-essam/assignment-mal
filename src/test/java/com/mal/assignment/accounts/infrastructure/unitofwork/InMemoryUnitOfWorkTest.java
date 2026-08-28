package com.mal.assignment.accounts.infrastructure.unitofwork;

import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commandhandlers.BookCreditCommandHandler;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.accounts.domain.services.BookingService;
import com.mal.assignment.accounts.infrastructure.buses.InMemoryCommandBus;
import com.mal.assignment.accounts.infrastructure.repositories.InMemoryAccountRepository;
import com.mal.assignment.support.StubUnitOfWork;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUnitOfWorkTest {

    @Test
    void commitInsertsNewAccountOnce() {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        InMemoryUnitOfWork first = new InMemoryUnitOfWork(repository);
        Account account = new Account("ACC-001", Currency.AED, 0L);
        first.registerNew(account);
        first.commit();
        assertTrue(repository.findById("ACC-001").isPresent());

        InMemoryUnitOfWork second = new InMemoryUnitOfWork(repository);
        second.registerNew(new Account("ACC-001", Currency.AED, 99L));
        second.commit();
        assertEquals(0L, repository.findById("ACC-001").orElseThrow().openingBalanceInMinorUnits());
    }

    @Test
    void commitFailsWhenAnotherUnitOfWorkAlreadySavedTheAccount() {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        InMemoryUnitOfWork open = new InMemoryUnitOfWork(repository);
        open.registerNew(new Account("ACC-001", Currency.AED, 0L));
        open.commit();

        InMemoryUnitOfWork first = new InMemoryUnitOfWork(repository);
        Account firstWorking = first.load("ACC-001").orElseThrow();
        firstWorking.appendLedgerEntry(LedgerEntry.draft(
                "E1", 1, 120_000L, LedgerEntryType.CREDIT, "E1"));

        InMemoryUnitOfWork second = new InMemoryUnitOfWork(repository);
        Account secondWorking = second.load("ACC-001").orElseThrow();
        secondWorking.appendLedgerEntry(LedgerEntry.draft(
                "E2", 1, 50_000L, LedgerEntryType.CREDIT, "E2"));

        first.commit();
        LedgerDomainException error = assertThrows(LedgerDomainException.class, second::commit);
        assertEquals(FailureReason.CONCURRENT_MODIFICATION, error.error().reason());
        assertEquals(120_000L, repository.findById("ACC-001").orElseThrow().amountInMinorUnits());
    }

    @Test
    void secondCommitIsADomainError() {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork(repository);
        unitOfWork.commit();
        LedgerDomainException error = assertThrows(LedgerDomainException.class, unitOfWork::commit);
        assertEquals(FailureReason.UNIT_OF_WORK_ALREADY_COMMITTED, error.error().reason());
    }

    @Test
    void handlerUsesStubUnitOfWorkNotARepository() {
        Account account = new Account("ACC-001", Currency.AED, 0L);
        StubUnitOfWork stub = new StubUnitOfWork(account);
        BookCreditCommandHandler handler = new BookCreditCommandHandler(
                new BookingService(stub, new InMemoryCommandBus()));
        CommandResult result = handler.handle(new BookCreditCommand("E1", "ACC-001", 1, 120_000L, "E1"));
        assertFalse(result.failed());
        assertEquals(1, stub.commitCount());
        assertEquals(120_000L, account.amountInMinorUnits());
    }

    @Test
    void loadReturnsEmptyForUnknownAccount() {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        InMemoryUnitOfWork unitOfWork = new InMemoryUnitOfWork(repository);
        assertTrue(unitOfWork.load("MISSING").isEmpty());
    }
}
