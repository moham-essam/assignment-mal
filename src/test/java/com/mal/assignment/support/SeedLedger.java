package com.mal.assignment.support;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;

public final class SeedLedger {

    private SeedLedger() {
    }

    public static void append(AccountsModule module, String accountId, LedgerEntry draft) {
        UnitOfWork unitOfWork = module.unitOfWorkFactory().begin();
        Account account = unitOfWork.load(accountId).orElseThrow();
        account.appendLedgerEntry(draft);
        unitOfWork.commit();
    }
}
