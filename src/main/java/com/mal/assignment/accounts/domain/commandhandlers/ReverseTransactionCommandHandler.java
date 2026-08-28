package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.ReverseTransactionCommand;
import com.mal.assignment.accounts.domain.services.ReversalService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ReverseTransactionCommandHandler implements CommandHandler<ReverseTransactionCommand> {

    private final ReversalService reversalService;

    @Override
    public CommandResult handle(ReverseTransactionCommand command) {
        return HandlerSupport.from(() -> reversalService.reverse(command));
    }
}
