package com.mal.assignment.accounts.infrastructure.repositories;

import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.models.Currency;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAccountRepositoryTest {

    @Test
    void findAllIdsIsSorted() {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        repository.putIfAbsent(new Account("ACC-002", Currency.BHD, 0L));
        repository.putIfAbsent(new Account("ACC-001", Currency.AED, 0L));
        assertEquals(List.of("ACC-001", "ACC-002"), repository.findAllIds());
    }

    @Test
    void findByIdIsEmptyWhenMissing() {
        assertTrue(new InMemoryAccountRepository().findById("MISSING").isEmpty());
    }
}
