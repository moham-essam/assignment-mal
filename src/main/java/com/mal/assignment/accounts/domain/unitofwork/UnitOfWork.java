package com.mal.assignment.accounts.domain.unitofwork;

import com.mal.assignment.accounts.domain.models.Account;

import java.util.Optional;

public interface UnitOfWork {

    Optional<Account> load(String accountId);

    void registerNew(Account account);

    long commit();
}
