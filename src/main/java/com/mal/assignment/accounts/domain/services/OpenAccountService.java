package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class OpenAccountService {

    private final UnitOfWorkFactory unitOfWorkFactory;

    public long open(OpenAccountCommand command) {
        UnitOfWork unitOfWork = unitOfWorkFactory.begin();
        Account account = new Account(
                command.accountId(),
                command.currency(),
                command.openingBalanceInMinorUnits()
        );
        unitOfWork.registerNew(account);
        return unitOfWork.commit();
    }
}
