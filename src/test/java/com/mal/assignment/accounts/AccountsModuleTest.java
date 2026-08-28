package com.mal.assignment.accounts;

import com.mal.assignment.accounts.domain.repositories.AccountRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountsModuleTest {

    @Test
    void handlersAreConstructedWithoutAccountRepository() {
        AccountsModule module = AccountsModule.shipped();
        for (Object handler : module.commandHandlers()) {
            for (Constructor<?> constructor : handler.getClass().getDeclaredConstructors()) {
                for (Class<?> parameter : constructor.getParameterTypes()) {
                    assertTrue(
                            !AccountRepository.class.isAssignableFrom(parameter),
                            handler.getClass().getSimpleName() + " must not take AccountRepository"
                    );
                }
            }
        }
    }
}
