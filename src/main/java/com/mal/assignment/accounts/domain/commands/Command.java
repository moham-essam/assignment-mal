package com.mal.assignment.accounts.domain.commands;

public sealed interface Command permits
        OpenAccountCommand,
        BookCreditCommand,
        BookDebitCommand,
        CreditInstalmentsCommand,
        AuthorizeCommand,
        SettleAuthorizationCommand,
        ReverseTransactionCommand,
        EndOfDayCommand,
        EndOfDayBatchCommand,
        ReconcileFromDayCommand,
        CapitalizeInterestCommand {

    String commandId();
}
