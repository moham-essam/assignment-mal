package com.mal.assignment.accounts.domain.commands;

public sealed interface Command permits
        OpenAccountCommand,
        BookCreditCommand,
        BookDebitCommand,
        AuthorizeCommand,
        SettleAuthorizationCommand {

    String commandId();
}
