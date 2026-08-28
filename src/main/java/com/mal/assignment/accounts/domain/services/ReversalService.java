package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.buses.CommandBus;
import com.mal.assignment.accounts.domain.commands.ReconcileFromDayCommand;
import com.mal.assignment.accounts.domain.commands.ReverseTransactionCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ReversalService {

    private final UnitOfWorkFactory unitOfWorkFactory;
    private final CommandBus commandBus;

    public long reverse(ReverseTransactionCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        LedgerEntry original = account.entryByReference(command.originalReferenceId()).orElse(null);
        if (original == null) {
            throw LedgerDomainException.of(
                    FailureReason.NOT_REVERSIBLE,
                    command.accountId(),
                    "No reversible entry " + command.originalReferenceId()
            );
        }
        if (account.alreadyReversed(command.originalReferenceId())) {
            throw LedgerDomainException.of(
                    FailureReason.ALREADY_REVERSED,
                    command.accountId(),
                    "Already reversed " + command.originalReferenceId()
            );
        }

        account.appendLedgerEntry(LedgerEntry.draft(
                command.commandId(),
                original.valueDay(),
                -original.signedAmountInMinorUnits(),
                LedgerEntryType.REVERSAL,
                command.originalReferenceId()
        ));

        long version = unitOfWork.commit();
        int lastClosedDay = account.lastClosedDay();
        if (lastClosedDay >= original.valueDay()) {
            commandBus.dispatch(new ReconcileFromDayCommand(
                    command.commandId() + ":recon",
                    command.accountId(),
                    original.valueDay(),
                    lastClosedDay
            ));
        }

        return version;
    }
}
