package com.mal.assignment.accounts;

import com.mal.assignment.accounts.domain.commandhandlers.AuthorizeCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.BookCreditCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.BookDebitCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.OpenAccountCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.SettleAuthorizationCommandHandler;
import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.commands.SettleAuthorizationCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.repositories.AccountRepository;
import com.mal.assignment.accounts.domain.services.AuthorizationService;
import com.mal.assignment.accounts.domain.services.BookingService;
import com.mal.assignment.accounts.domain.services.OpenAccountService;
import com.mal.assignment.accounts.domain.unitofwork.UnitOfWorkFactory;
import com.mal.assignment.accounts.infrastructure.buses.InMemoryCommandBus;
import com.mal.assignment.accounts.infrastructure.config.LedgerConfig;
import com.mal.assignment.accounts.infrastructure.config.YamlLedgerConfigLoader;
import com.mal.assignment.accounts.infrastructure.repositories.InMemoryAccountRepository;
import com.mal.assignment.accounts.infrastructure.unitofwork.InMemoryUnitOfWorkFactory;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

public final class AccountsModule {

    private final AccountRepository accountRepository;
    @Getter
    private final UnitOfWorkFactory unitOfWorkFactory;
    @Getter
    private final InMemoryCommandBus commandBus;
    @Getter
    private final LedgerConfig ledgerConfig;
    private final OpenAccountCommandHandler openAccountCommandHandler;
    private final BookCreditCommandHandler bookCreditCommandHandler;
    private final BookDebitCommandHandler bookDebitCommandHandler;
    private final AuthorizeCommandHandler authorizeCommandHandler;
    private final SettleAuthorizationCommandHandler settleAuthorizationCommandHandler;

    public static AccountsModule shipped() {
        return new AccountsModule(new YamlLedgerConfigLoader().loadFromClasspath(), new InMemoryAccountRepository());
    }

    public AccountsModule(LedgerConfig ledgerConfig, AccountRepository accountRepository) {
        this.ledgerConfig = ledgerConfig;
        this.accountRepository = accountRepository;
        this.unitOfWorkFactory = new InMemoryUnitOfWorkFactory(accountRepository);
        this.commandBus = new InMemoryCommandBus();
        OpenAccountService openAccountService = new OpenAccountService(unitOfWorkFactory);
        BookingService bookingService = new BookingService(unitOfWorkFactory);
        AuthorizationService authorizationService = new AuthorizationService(unitOfWorkFactory);
        this.openAccountCommandHandler = new OpenAccountCommandHandler(openAccountService);
        this.bookCreditCommandHandler = new BookCreditCommandHandler(bookingService);
        this.bookDebitCommandHandler = new BookDebitCommandHandler(bookingService);
        this.authorizeCommandHandler = new AuthorizeCommandHandler(authorizationService);
        this.settleAuthorizationCommandHandler = new SettleAuthorizationCommandHandler(authorizationService);
        commandBus.register(OpenAccountCommand.class, openAccountCommandHandler);
        commandBus.register(BookCreditCommand.class, bookCreditCommandHandler);
        commandBus.register(BookDebitCommand.class, bookDebitCommandHandler);
        commandBus.register(AuthorizeCommand.class, authorizeCommandHandler);
        commandBus.register(SettleAuthorizationCommand.class, settleAuthorizationCommandHandler);
    }

    public Optional<Account> account(String accountId) {
        return accountRepository.findById(accountId);
    }

    public List<Object> commandHandlers() {
        return List.of(
                openAccountCommandHandler,
                bookCreditCommandHandler,
                bookDebitCommandHandler,
                authorizeCommandHandler,
                settleAuthorizationCommandHandler
        );
    }
}
