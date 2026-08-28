package com.mal.assignment.accounts.domain.buses;

import com.mal.assignment.accounts.domain.commands.Command;
import com.mal.assignment.accounts.domain.commands.CommandResult;

public interface CommandBus {

    CommandResult dispatch(Command command);
}
