package com.mal.assignment.simulation;

import com.mal.assignment.accounts.domain.buses.CommandBus;
import com.mal.assignment.accounts.domain.commands.CapitalizeInterestCommand;
import com.mal.assignment.accounts.domain.commands.Command;
import com.mal.assignment.accounts.domain.commands.CommandResult;
import com.mal.assignment.accounts.domain.commands.EndOfDayBatchCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.infrastructure.config.AccountSeedConfig;
import com.mal.assignment.accounts.infrastructure.config.LedgerConfig;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

@RequiredArgsConstructor
public final class SimulationEngine {

    private final CommandBus commandBus;
    private final LedgerConfig ledgerConfig;

    public SimulationResult run(List<ScheduledCommand> schedule) {
        return run(schedule, day -> {
        });
    }

    public SimulationResult run(List<ScheduledCommand> schedule, IntConsumer afterDay) {
        List<CommandResult> results = new ArrayList<>();
        List<String> dispatchedIds = new ArrayList<>();
        for (AccountSeedConfig seed : ledgerConfig.accounts()) {
            dispatch(new OpenAccountCommand(
                    "open:" + seed.id(),
                    seed.id(),
                    Currency.fromCode(seed.currency()),
                    seed.openingBalanceInMinorUnits()
            ), results, dispatchedIds);
        }
        int index = 0;
        int windowDays = ledgerConfig.windowDays();
        for (int day = 1; day <= windowDays; day++) {
            while (index < schedule.size() && schedule.get(index).arrivalDay() == day) {
                dispatch(schedule.get(index).command(), results, dispatchedIds);
                index++;
            }
            dispatch(new EndOfDayBatchCommand("EOD-D" + day, day), results, dispatchedIds);
            afterDay.accept(day);
        }
        int capitalizationDay = ledgerConfig.interest().capitalizationDay();
        for (AccountSeedConfig seed : ledgerConfig.accounts()) {
            dispatch(new CapitalizeInterestCommand(
                    "CAPITALIZE:" + seed.id(),
                    seed.id(),
                    capitalizationDay
            ), results, dispatchedIds);
        }
        return new SimulationResult(List.copyOf(results), List.copyOf(dispatchedIds), List.of());
    }

    private CommandResult dispatch(Command command, List<CommandResult> results, List<String> dispatchedIds) {
        dispatchedIds.add(command.commandId());
        CommandResult result = commandBus.dispatch(command);
        results.add(result);
        return result;
    }
}
