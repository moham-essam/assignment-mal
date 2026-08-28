package com.mal.assignment.accounts.infrastructure.unitofwork;

import com.mal.assignment.accounts.domain.repositories.AccountRepository;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class InMemoryUnitOfWorkFactory implements UnitOfWorkFactory {

    private final AccountRepository accountRepository;

    @Override
    public UnitOfWork begin() {
        return new InMemoryUnitOfWork(accountRepository);
    }
}
