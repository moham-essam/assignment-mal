package com.mal.assignment.accounts.infrastructure.buses;

import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.models.Currency;
import com.mal.assignment.accounts.domain.models.FailureReason;
import com.mal.assignment.accounts.domain.models.LedgerDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryCommandBusTest {

    @Test
    void unknownHandlerIsADomainError() {
        InMemoryCommandBus bus = new InMemoryCommandBus();
        LedgerDomainException error = assertThrows(
                LedgerDomainException.class,
                () -> bus.dispatch(new OpenAccountCommand("open", "ACC-001", Currency.AED, 0L))
        );
        assertEquals(FailureReason.UNKNOWN_COMMAND_HANDLER, error.error().reason());
    }
}
