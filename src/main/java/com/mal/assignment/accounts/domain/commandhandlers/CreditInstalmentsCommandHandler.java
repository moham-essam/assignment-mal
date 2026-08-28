package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.CreditInstalmentsCommand;
import com.mal.assignment.accounts.domain.services.BookingService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CreditInstalmentsCommandHandler implements CommandHandler<CreditInstalmentsCommand> {

    private final BookingService bookingService;

    @Override
    public CommandResult handle(CreditInstalmentsCommand command) {
        return HandlerSupport.from(() -> bookingService.creditInstalments(command));
    }
}
