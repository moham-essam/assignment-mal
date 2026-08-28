package com.mal.assignment.accounts.infrastructure.repositories;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.repositories.AccountRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountRepository implements AccountRepository {

    private final ConcurrentHashMap<String, StoredAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public List<String> findAllIds() {
        return accounts.keySet().stream().sorted().toList();
    }

    @Override
    public Optional<Account> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId)).map(StoredAccount::workingCopy);
    }

    @Override
    public Optional<Account> snapshot(String accountId) {
        return Optional.ofNullable(accounts.get(accountId)).map(StoredAccount::committed);
    }

    @Override
    public boolean putIfAbsent(Account account) {
        return accounts.putIfAbsent(account.id(), StoredAccount.from(account)) == null;
    }

    @Override
    public boolean compareAndSet(String accountId, Account expected, Account next) {
        StoredAccount stored = accounts.get(accountId);
        return stored != null && stored.compareAndSet(expected, next);
    }
}
