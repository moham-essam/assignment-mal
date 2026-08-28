package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.SettleAuthorizationCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Authorization;
import com.mal.assignment.accounts.domain.models.AuthorizationStatus;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.InsufficientAvailableBalanceException;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerEntryType;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AuthorizationService {

    private final UnitOfWorkFactory unitOfWorkFactory;

    public long authorize(AuthorizeCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        try {
            account.ensureSufficientAvailable(command.requestedAmountInMinorUnits());
        } catch (InsufficientAvailableBalanceException exception) {
            account.appendAuthorization(new Authorization(
                    command.commandId(),
                    command.authorizationReference(),
                    command.requestedAmountInMinorUnits(),
                    AuthorizationStatus.DECLINED
            ));
            unitOfWork.commit();
            throw exception;
        }

        account.appendAuthorization(new Authorization(
                command.commandId(),
                command.authorizationReference(),
                command.requestedAmountInMinorUnits(),
                AuthorizationStatus.APPROVED
        ));

        return unitOfWork.commit();
    }

    public long settle(SettleAuthorizationCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = RequiredAccount.load(unitOfWork, command.accountId());

        Authorization authorization = account.authorizationByReference(command.authorizationReference())
                .orElse(null);
        if (authorization == null || !authorization.approved()) {
            throw LedgerDomainException.of(
                    FailureReason.UNKNOWN_AUTHORIZATION,
                    command.accountId(),
                    "No approved authorization " + command.authorizationReference()
            );
        }

        account.appendLedgerEntry(LedgerEntry.draft(
                command.commandId(),
                command.valueDay(),
                -command.amountInMinorUnits(),
                LedgerEntryType.SETTLEMENT,
                command.authorizationReference()
        ));
        account.releaseHold(authorization.requestedAmountInMinorUnits());

        return unitOfWork.commit();
    }
}
