package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.commands.CapitalizeInterestCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayBatchCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.ReconcileFromDayCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.accounts.domain.repositories.AccountRepository;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class EndOfDayService {

    private final UnitOfWorkFactory unitOfWorkFactory;
    private final AccountRepository accountRepository;
    private final OverdraftPolicy overdraftPolicy;
    private final InterestPolicy interestPolicy;

    public long closeDay(EndOfDayCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        long closing = account.postingsOnlyClosing(command.day());
        long netFee = account.netFeeSignedAmount(command.day());
        long fee = overdraftPolicy.feeInMinorUnits(account.currency());
        if (closing < 0 && netFee == 0L) {
            account.appendLedgerEntry(LedgerEntry.draft(
                    "OD-FEE:" + account.id() + ":" + command.day(),
                    command.day(),
                    -fee,
                    LedgerEntryType.OVERDRAFT_FEE,
                    "OD-FEE-" + command.day()
            ));
            account.accrueInterestForDay(command.day(), 0L);
        } else {
            account.accrueInterestForDay(command.day(), interestPolicy);
        }

        return unitOfWork.commit();
    }

    public long reconcile(ReconcileFromDayCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        long fee = overdraftPolicy.feeInMinorUnits(account.currency());
        long running = account.closingBalance(command.fromDay() - 1);
        for (int day = command.fromDay(); day <= command.toDay(); day++) {
            long dayPostings = 0L;
            for (LedgerEntry entry : account.ledgerEntriesSortedByValueDay(day)) {
                if (entry.valueDay() > day) {
                    break;
                }
                if (!entry.type().ownDayFee()) {
                    dayPostings += entry.signedAmountInMinorUnits();
                }
            }
            long closing = running + dayPostings;
            long netFee = account.netFeeSignedAmount(day);
            if (closing < 0 && netFee == 0L) {
                account.appendLedgerEntry(LedgerEntry.draft(
                        "OD-FEE:" + account.id() + ":" + day + ":" + account.entries().size(),
                        day,
                        -fee,
                        LedgerEntryType.OVERDRAFT_FEE,
                        "OD-FEE-" + day
                ));
            } else if (closing >= 0 && netFee < 0L) {
                account.appendLedgerEntry(LedgerEntry.draft(
                        "OD-FEE-REV:" + account.id() + ":" + day + ":" + account.entries().size(),
                        day,
                        -netFee,
                        LedgerEntryType.FEE_REVERSAL,
                        "OD-FEE-REV-" + day
                ));
            }
            running = closing + account.netFeeSignedAmount(day);
            account.accrueInterestForDay(day, interestPolicy.dailyAccrual(closing));
        }

        return unitOfWork.commit();
    }

    public long capitalize(CapitalizeInterestCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        long capitalized = account.summedAccruals();
        if (capitalized != 0L) {
            account.appendLedgerEntry(LedgerEntry.draft(
                    command.commandId(),
                    command.valueDay(),
                    capitalized,
                    LedgerEntryType.INTEREST_CAPITALIZATION,
                    command.commandId()
            ));
        }

        return unitOfWork.commit();
    }

    public long closeDayBatch(EndOfDayBatchCommand command) {
        long last = 0L;
        for (String accountId : accountRepository.findAllIds()) {
            last = closeDay(new EndOfDayCommand(
                    command.commandId() + ":" + accountId,
                    accountId,
                    command.day()
            ));
        }
        return last;
    }
}
