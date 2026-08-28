package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.services.OpenAccountService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class OpenAccountCommandHandler implements CommandHandler<OpenAccountCommand> {

    private final OpenAccountService openAccountService;

    @Override
    public CommandResult handle(OpenAccountCommand command) {
        return HandlerSupport.from(() -> openAccountService.open(command));
    }
}
