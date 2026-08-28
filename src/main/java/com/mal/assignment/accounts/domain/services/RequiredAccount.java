package com.mal.assignment.accounts.domain.services;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWork;

final class RequiredAccount {

    private RequiredAccount() {
    }

    static Account load(UnitOfWork unitOfWork, String accountId) {
        return unitOfWork.load(accountId).orElseThrow(() -> LedgerDomainException.of(
                FailureReason.UNKNOWN_ACCOUNT,
                accountId,
                "Unknown account " + accountId
        ));
    }
}
