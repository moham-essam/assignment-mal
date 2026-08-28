package com.mal.assignment.accounts.domain.repositories;

import com.mal.assignment.accounts.domain.models.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    List<String> findAllIds();

    Optional<Account> findById(String accountId);

    Optional<Account> snapshot(String accountId);

    boolean putIfAbsent(Account account);

    boolean compareAndSet(String accountId, Account expected, Account next);
}
