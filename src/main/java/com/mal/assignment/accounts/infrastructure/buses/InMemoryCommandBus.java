package com.mal.assignment.accounts.infrastructure.buses;

import com.mal.assignment.accounts.domain.buses.CommandBus;
import com.mal.assignment.accounts.domain.commandhandlers.CommandHandler;
import com.mal.assignment.accounts.domain.commands.Command;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCommandBus implements CommandBus {

    private final Map<Class<? extends Command>, CommandHandler<?>> handlers = new ConcurrentHashMap<>();

    public <C extends Command> void register(Class<C> type, CommandHandler<C> handler) {
        handlers.put(type, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CommandResult dispatch(Command command) {
        CommandHandler<Command> handler = (CommandHandler<Command>) handlers.get(command.getClass());
        if (handler == null) {
            throw LedgerDomainException.of(
                    FailureReason.UNKNOWN_COMMAND_HANDLER,
                    "No handler registered for " + command.getClass().getSimpleName()
            );
        }
        return handler.handle(command);
    }
}
