package com.mal.assignment.accounts.domain.commandhandlers;

import com.mal.assignment.accounts.domain.commands.Command;
import com.mal.assignment.accounts.domain.commands.CommandResult;

public interface CommandHandler<C extends Command> {

    CommandResult handle(C command);
}
