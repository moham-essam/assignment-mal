package com.mal.assignment.accounts.domain.services;

public final class ReconciliationTrigger {

    public boolean shouldReconcile(int valueDay, int lastClosedDay) {
        return lastClosedDay >= valueDay;
    }
}
