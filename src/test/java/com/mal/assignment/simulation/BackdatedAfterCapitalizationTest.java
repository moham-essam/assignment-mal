package com.mal.assignment.simulation;

import com.mal.assignment.accounts.AccountsModule;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.ReconcileFromDayCommand;
import com.mal.assignment.accounts.domain.models.Account;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("known-limitation")
class BackdatedAfterCapitalizationTest {

    @Test
    void capitalizationTracksAccrualsAfterABackdatedPostingOnDaySix() {
        AccountsModule module = AccountsModule.shipped();
        new SimulationEngine(module.commandBus(), module.ledgerConfig())
                .run(ScenarioFixture.assignment());
        module.commandBus().dispatch(new BookCreditCommand("late", "ACC-001", 2, 100_000L, "late"));
        module.commandBus().dispatch(new ReconcileFromDayCommand("late-recon", "ACC-001", 2, 6));
        Account account = module.account("ACC-001").orElseThrow();
        assertEquals(account.summedAccruals(), account.capitalizedInterestInMinorUnits());
    }
}
