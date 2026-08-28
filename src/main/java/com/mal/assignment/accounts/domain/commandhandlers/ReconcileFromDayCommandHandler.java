package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.ReconcileFromDayCommand;
import com.mal.assignment.accounts.domain.services.EndOfDayService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ReconcileFromDayCommandHandler implements CommandHandler<ReconcileFromDayCommand> {

    private final EndOfDayService endOfDayService;

    @Override
    public CommandResult handle(ReconcileFromDayCommand command) {
        return HandlerSupport.from(() -> endOfDayService.reconcile(command));
    }
}
