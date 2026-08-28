package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.services.BookingService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BookCreditCommandHandler implements CommandHandler<BookCreditCommand> {

    private final BookingService bookingService;

    @Override
    public CommandResult handle(BookCreditCommand command) {
        return HandlerSupport.from(() -> bookingService.bookCredit(command));
    }
}
