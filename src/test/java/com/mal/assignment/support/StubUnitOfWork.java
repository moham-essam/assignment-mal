package com.mal.assignment.support;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public final class StubUnitOfWork implements UnitOfWork, UnitOfWorkFactory {

    private final Account account;
    private final RuntimeException commitFailure;
    @Getter
    private int commitCount;
    @Getter
    private boolean registerCalled;

    public StubUnitOfWork(Account account) {
        this(account, null);
    }

    @Override
    public UnitOfWork begin() {
        return this;
    }

    @Override
    public Optional<Account> load(String accountId) {
        if (account != null && account.id().equals(accountId)) {
            return Optional.of(account);
        }
        return Optional.empty();
    }

    @Override
    public void registerNew(Account account) {
        registerCalled = true;
    }

    @Override
    public long commit() {
        commitCount++;
        if (commitFailure != null) {
            throw commitFailure;
        }
        return account == null ? 0L : account.version();
    }
}
