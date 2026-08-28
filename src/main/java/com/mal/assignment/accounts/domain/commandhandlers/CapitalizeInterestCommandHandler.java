package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CapitalizeInterestCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.services.EndOfDayService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CapitalizeInterestCommandHandler implements CommandHandler<CapitalizeInterestCommand> {

    private final EndOfDayService endOfDayService;

    @Override
    public CommandResult handle(CapitalizeInterestCommand command) {
        return HandlerSupport.from(() -> endOfDayService.capitalize(command));
    }
}
