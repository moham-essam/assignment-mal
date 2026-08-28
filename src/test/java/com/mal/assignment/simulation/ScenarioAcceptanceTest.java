package com.mal.assignment.simulation;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerEntry;
import com.mal.assignment.accounts.domain.models.LedgerError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioAcceptanceTest {

    @Test
    void assignmentStreamIsE1ThroughE10InOrder() {
        SimulationResult result = replay();
        List<String> ids = result.dispatchedCommandIds();
        assertTrue(index(ids, "E1") < index(ids, "E2"));
        assertTrue(index(ids, "E2") < index(ids, "E3"));
        assertTrue(index(ids, "E3") < index(ids, "E4"));
        assertTrue(index(ids, "E4") < index(ids, "E5"));
        assertTrue(index(ids, "E5") < index(ids, "E6"));
        assertTrue(index(ids, "E6") < index(ids, "E7"));
        assertTrue(index(ids, "E7") < index(ids, "E8"));
        assertTrue(index(ids, "E8") < index(ids, "E9"));
        assertTrue(index(ids, "E9") < index(ids, "E10"));
        assertTrue(index(ids, "EOD-D1") < index(ids, "E3"));
        assertTrue(index(ids, "EOD-D5") < index(ids, "E9"));
        assertTrue(index(ids, "E10") < index(ids, "EOD-D6"));
        assertTrue(index(ids, "EOD-D6") < index(ids, "CAPITALIZE:ACC-001"));
    }

    @Test
    void engineDoesNotDispatchReconcile() {
        List<String> ids = replay().dispatchedCommandIds();
        assertFalse(ids.contains("E7:recon"));
        assertFalse(ids.contains("E10:recon"));
        assertFalse(ids.contains("E9:recon"));
    }

    @Test
    void assignmentReplayBooksThenReversesE7() {
        AccountsModule module = AccountsModule.shipped();
        new SimulationEngine(module.commandBus(), module.ledgerConfig())
                .run(ScenarioFixture.assignment());
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(46_600L, account.amountInMinorUnits());
        assertTrue(account.entryByReference("E7").isPresent());
        assertTrue(account.alreadyReversed("E7"));
    }

    @Test
    void conservationHoldsForBothAccounts() {
        AccountsModule module = AccountsModule.shipped();
        new SimulationEngine(module.commandBus(), module.ledgerConfig())
                .run(ScenarioFixture.assignment());
        assertConserved(module.account("ACC-001").orElseThrow());
        assertConserved(module.account("ACC-002").orElseThrow());
    }

    @Test
    void assignmentFailuresAreE6AndE8() {
        List<FailureReason> reasons = replay().errors().stream().map(LedgerError::reason).toList();
        assertEquals(
                List.of(
                        FailureReason.UNKNOWN_AUTHORIZATION,
                        FailureReason.INSUFFICIENT_AVAILABLE_BALANCE
                ),
                reasons
        );
    }

    private static SimulationResult replay() {
        AccountsModule module = AccountsModule.shipped();
        return new SimulationEngine(module.commandBus(), module.ledgerConfig())
                .run(ScenarioFixture.assignment());
    }

    private static int index(List<String> ids, String commandId) {
        int index = ids.indexOf(commandId);
        assertTrue(index >= 0, "missing " + commandId);
        return index;
    }

    private static void assertConserved(Account account) {
        long summed = account.openingBalanceInMinorUnits();
        for (LedgerEntry entry : account.entries()) {
            summed += entry.signedAmountInMinorUnits();
        }
        assertEquals(summed, account.amountInMinorUnits());
        assertEquals(summed, account.closingBalance(6));
    }
}
