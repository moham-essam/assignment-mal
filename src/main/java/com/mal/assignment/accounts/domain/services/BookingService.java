package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BookingService {

    private final UnitOfWorkFactory unitOfWorkFactory;

    public long bookCredit(BookCreditCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        account.appendLedgerEntry(LedgerEntry.draft(
                command.commandId(),
                command.valueDay(),
                command.amountInMinorUnits(),
                LedgerEntryType.CREDIT,
                command.referenceId()
        ));

        return unitOfWork.commit();
    }

    public long bookDebit(BookDebitCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        account.ensureSufficientAvailable(command.amountInMinorUnits());
        account.appendLedgerEntry(LedgerEntry.draft(
                command.commandId(),
                command.valueDay(),
                -command.amountInMinorUnits(),
                LedgerEntryType.DEBIT,
                command.referenceId()
        ));

        return unitOfWork.commit();
    }
}
