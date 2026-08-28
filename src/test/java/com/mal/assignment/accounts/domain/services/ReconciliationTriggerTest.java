package com.mal.assignment.accounts.domain.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationTriggerTest {

    @Test
    void reconcilesWhenTheValueDayIsAlreadyClosed() {
        ReconciliationTrigger trigger = new ReconciliationTrigger();
        assertTrue(trigger.shouldReconcile(2, 4));
        assertTrue(trigger.shouldReconcile(5, 5));
        assertFalse(trigger.shouldReconcile(4, 3));
        assertFalse(trigger.shouldReconcile(1, 0));
    }
}
