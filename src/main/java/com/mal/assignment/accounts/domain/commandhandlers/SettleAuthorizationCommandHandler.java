package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.SettleAuthorizationCommand;
import com.mal.assignment.accounts.domain.services.AuthorizationService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SettleAuthorizationCommandHandler implements CommandHandler<SettleAuthorizationCommand> {

    private final AuthorizationService authorizationService;

    @Override
    public CommandResult handle(SettleAuthorizationCommand command) {
        return HandlerSupport.from(() -> authorizationService.settle(command));
    }
}
