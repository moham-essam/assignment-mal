package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.buses.CommandBus;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.CreditInstalmentsCommand;
import com.mal.assignment.accounts.domain.commands.ReconcileFromDayCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.accounts.domain.models.MinorUnits;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BookingService {

    private final UnitOfWorkFactory unitOfWorkFactory;
    private final CommandBus commandBus;

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

        return commitAndReconcile(unitOfWork, account, command.commandId(), command.accountId(), command.valueDay());
    }

    public long bookDebit(BookDebitCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        account.appendLedgerEntry(LedgerEntry.draft(
                command.commandId(),
                command.valueDay(),
                -command.amountInMinorUnits(),
                LedgerEntryType.DEBIT,
                command.referenceId()
        ));

        return commitAndReconcile(unitOfWork, account, command.commandId(), command.accountId(), command.valueDay());
    }

    public long creditInstalments(CreditInstalmentsCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        long[] splits = MinorUnits.splitEvenly(command.totalAmountInMinorUnits(), command.parts());
        for (int i = 0; i < splits.length; i++) {
            String instalmentId = command.commandId() + ":" + (i + 1);
            account.appendLedgerEntry(LedgerEntry.draft(
                    instalmentId,
                    command.valueDay(),
                    splits[i],
                    LedgerEntryType.CREDIT,
                    instalmentId
            ));
        }

        return commitAndReconcile(unitOfWork, account, command.commandId(), command.accountId(), command.valueDay());
    }

    private long commitAndReconcile(
            UnitOfWork unitOfWork,
            Account account,
            String commandId,
            String accountId,
            int valueDay
    ) {
        long version = unitOfWork.commit();
        int lastClosedDay = account.lastClosedDay();
        if (lastClosedDay >= valueDay) {
            commandBus.dispatch(new ReconcileFromDayCommand(
                    commandId + ":recon",
                    accountId,
                    valueDay,
                    lastClosedDay
            ));
        }
        return version;
    }
}
