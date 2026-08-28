package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.EndOfDayBatchCommand;
import com.mal.assignment.accounts.domain.services.EndOfDayService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class EndOfDayBatchCommandHandler implements CommandHandler<EndOfDayBatchCommand> {

    private final EndOfDayService endOfDayService;

    @Override
    public CommandResult handle(EndOfDayBatchCommand command) {
        return HandlerSupport.from(() -> endOfDayService.closeDayBatch(command));
    }
}
