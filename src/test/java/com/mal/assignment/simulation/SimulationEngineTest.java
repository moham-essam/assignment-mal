package com.mal.assignment.simulation;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    @Test
    void assignmentReplayClosesThroughCapitalizationAndRecordsDeclinedCommands() {
        AccountsModule module = AccountsModule.shipped();
        SimulationResult result = new SimulationEngine(module.commandBus(), module.ledgerConfig())
                .run(ScenarioFixture.assignment());
        List<FailureReason> reasons = result.errors().stream().map(LedgerError::reason).toList();
        assertTrue(reasons.contains(FailureReason.UNKNOWN_AUTHORIZATION));
        assertTrue(reasons.contains(FailureReason.INSUFFICIENT_AVAILABLE_BALANCE));
        Account acc001 = module.account("ACC-001").orElseThrow();
        assertEquals(25_000L, acc001.closingBalance(1));
        assertEquals(25_000L, acc001.closingBalance(2));
        assertEquals(65_000L, acc001.closingBalance(3));
        assertEquals(46_500L, acc001.closingBalance(4));
        assertEquals(46_500L, acc001.closingBalance(5));
        assertEquals(46_600L, acc001.closingBalance(6));
        assertEquals(100L, acc001.capitalizedInterestInMinorUnits());
        Account acc002 = module.account("ACC-002").orElseThrow();
        assertEquals(10_000L, acc002.closingBalance(5));
        assertEquals(10_008L, acc002.closingBalance(6));
        assertEquals(8L, acc002.capitalizedInterestInMinorUnits());
    }

    @Test
    void notifiesAfterEachClosedDay() {
        AccountsModule module = AccountsModule.shipped();
        List<Integer> closed = new ArrayList<>();
        new SimulationEngine(module.commandBus(), module.ledgerConfig())
                .run(ScenarioFixture.assignment(), closed::add);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), closed);
    }

    @Test
    void assignmentReplayIsDeterministic() {
        AccountsModule firstModule = AccountsModule.shipped();
        AccountsModule secondModule = AccountsModule.shipped();
        new SimulationEngine(firstModule.commandBus(), firstModule.ledgerConfig())
                .run(ScenarioFixture.assignment());
        new SimulationEngine(secondModule.commandBus(), secondModule.ledgerConfig())
                .run(ScenarioFixture.assignment());
        Account first = firstModule.account("ACC-001").orElseThrow();
        Account second = secondModule.account("ACC-001").orElseThrow();
        assertEquals(first.closingBalance(6), second.closingBalance(6));
        assertEquals(first.amountInMinorUnits(), second.amountInMinorUnits());
        assertEquals(first.capitalizedInterestInMinorUnits(), second.capitalizedInterestInMinorUnits());
    }
}
