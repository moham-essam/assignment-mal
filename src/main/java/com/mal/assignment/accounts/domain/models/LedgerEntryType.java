package com.mal.assignment.accounts.domain.models;

public enum LedgerEntryType {
    CREDIT,
    DEBIT,
    SETTLEMENT,
    REVERSAL,
    OVERDRAFT_FEE,
    FEE_REVERSAL,
    INTEREST_CAPITALIZATION;

    public boolean ownDayFee() {
        return this == OVERDRAFT_FEE || this == FEE_REVERSAL;
    }
}
