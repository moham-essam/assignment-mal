package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.services.AuthorizationService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AuthorizeCommandHandler implements CommandHandler<AuthorizeCommand> {

    private final AuthorizationService authorizationService;

    @Override
    public CommandResult handle(AuthorizeCommand command) {
        return HandlerSupport.from(() -> authorizationService.authorize(command));
    }
}
