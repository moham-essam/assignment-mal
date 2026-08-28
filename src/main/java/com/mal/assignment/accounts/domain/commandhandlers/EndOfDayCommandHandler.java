package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.services.EndOfDayService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class EndOfDayCommandHandler implements CommandHandler<EndOfDayCommand> {

    private final EndOfDayService endOfDayService;

    @Override
    public CommandResult handle(EndOfDayCommand command) {
        return HandlerSupport.from(() -> endOfDayService.closeDay(command));
    }
}
