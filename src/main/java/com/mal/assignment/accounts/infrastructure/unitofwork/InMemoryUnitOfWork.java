package com.mal.assignment.accounts.infrastructure.unitofwork;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import com.mal.assignment.accounts.domain.repositories.AccountRepository;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public final class InMemoryUnitOfWork implements UnitOfWork {

    private final AccountRepository accountRepository;
    private final List<Account> registered = new ArrayList<>();
    private final AtomicReference<Account> existingSnapshot = new AtomicReference<>();
    private Account loaded;
    private boolean committed;

    @Override
    public Optional<Account> load(String accountId) {
        Optional<Account> committedSnapshot = accountRepository.snapshot(accountId);
        if (committedSnapshot.isEmpty()) {
            return Optional.empty();
        }
        existingSnapshot.set(committedSnapshot.get());
        loaded = accountRepository.findById(accountId).orElseThrow();
        return Optional.of(loaded);
    }

    @Override
    public void registerNew(Account account) {
        registered.add(account);
        loaded = account;
    }

    @Override
    public long commit() {
        if (committed) {
            throw LedgerDomainException.of(
                    FailureReason.UNIT_OF_WORK_ALREADY_COMMITTED,
                    "Unit of work already committed"
            );
        }
        committed = true;
        for (Account account : registered) {
            if (!accountRepository.putIfAbsent(account)) {
                loaded = accountRepository.findById(account.id()).orElse(account);
            }
        }
        Account expected = existingSnapshot.get();
        if (expected != null && loaded != null) {
            if (!accountRepository.compareAndSet(loaded.id(), expected, loaded)) {
                throw LedgerDomainException.of(
                        FailureReason.CONCURRENT_MODIFICATION,
                        loaded.id(),
                        "Account " + loaded.id() + " was modified concurrently"
                );
            }
            existingSnapshot.set(accountRepository.snapshot(loaded.id()).orElse(loaded));
        }
        return loaded == null ? 0L : loaded.version();
    }
}
