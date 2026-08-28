package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.services.BookingService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BookDebitCommandHandler implements CommandHandler<BookDebitCommand> {

    private final BookingService bookingService;

    @Override
    public CommandResult handle(BookDebitCommand command) {
        return HandlerSupport.from(() -> bookingService.bookDebit(command));
    }
}
