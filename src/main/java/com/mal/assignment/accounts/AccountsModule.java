package com.mal.assignment.accounts;

import com.mal.assignment.accounts.domain.commandhandlers.AuthorizeCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.BookCreditCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.BookDebitCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.CapitalizeInterestCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.CreditInstalmentsCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.EndOfDayBatchCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.EndOfDayCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.OpenAccountCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.ReconcileFromDayCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.ReverseTransactionCommandHandler;
import com.mal.assignment.accounts.domain.commandhandlers.SettleAuthorizationCommandHandler;
import com.mal.assignment.accounts.domain.commands.AuthorizeCommand;
import com.mal.assignment.accounts.domain.commands.BookCreditCommand;
import com.mal.assignment.accounts.domain.commands.BookDebitCommand;
import com.mal.assignment.accounts.domain.commands.CapitalizeInterestCommand;
import com.mal.assignment.accounts.domain.commands.CreditInstalmentsCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayBatchCommand;
import com.mal.assignment.accounts.domain.commands.EndOfDayCommand;
import com.mal.assignment.accounts.domain.commands.OpenAccountCommand;
import com.mal.assignment.accounts.domain.commands.ReconcileFromDayCommand;
import com.mal.assignment.accounts.domain.commands.ReverseTransactionCommand;
import com.mal.assignment.accounts.domain.commands.SettleAuthorizationCommand;
import com.mal.assignment.accounts.domain.models.Account;
import com.mal.assignment.accounts.domain.repositories.AccountRepository;
import com.mal.assignment.accounts.domain.services.AuthorizationService;
import com.mal.assignment.accounts.domain.services.BookingService;
import com.mal.assignment.accounts.domain.services.EndOfDayService;
import com.mal.assignment.accounts.domain.services.LedgerPolicies;
import com.mal.assignment.accounts.domain.services.OpenAccountService;
import com.mal.assignment.accounts.domain.services.ReversalService;
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
    @Getter
    private final LedgerPolicies ledgerPolicies;
    private final OpenAccountCommandHandler openAccountCommandHandler;
    private final BookCreditCommandHandler bookCreditCommandHandler;
    private final BookDebitCommandHandler bookDebitCommandHandler;
    private final CreditInstalmentsCommandHandler creditInstalmentsCommandHandler;
    private final AuthorizeCommandHandler authorizeCommandHandler;
    private final SettleAuthorizationCommandHandler settleAuthorizationCommandHandler;
    private final ReverseTransactionCommandHandler reverseTransactionCommandHandler;
    private final EndOfDayCommandHandler endOfDayCommandHandler;
    private final ReconcileFromDayCommandHandler reconcileFromDayCommandHandler;
    private final CapitalizeInterestCommandHandler capitalizeInterestCommandHandler;
    private final EndOfDayBatchCommandHandler endOfDayBatchCommandHandler;

    public static AccountsModule shipped() {
        return new AccountsModule(new YamlLedgerConfigLoader().loadFromClasspath(), new InMemoryAccountRepository());
    }

    public AccountsModule(LedgerConfig ledgerConfig, AccountRepository accountRepository) {
        this.ledgerConfig = ledgerConfig;
        this.accountRepository = accountRepository;
        this.unitOfWorkFactory = new InMemoryUnitOfWorkFactory(accountRepository);
        this.ledgerPolicies = new LedgerPolicies(ledgerConfig);
        this.commandBus = new InMemoryCommandBus();
        OpenAccountService openAccountService = new OpenAccountService(unitOfWorkFactory);
        BookingService bookingService = new BookingService(unitOfWorkFactory, commandBus);
        AuthorizationService authorizationService = new AuthorizationService(unitOfWorkFactory, commandBus);
        ReversalService reversalService = new ReversalService(unitOfWorkFactory, commandBus);
        EndOfDayService endOfDayService = new EndOfDayService(
                unitOfWorkFactory,
                accountRepository,
                ledgerPolicies.overdraftPolicy(),
                ledgerPolicies.interestPolicy()
        );
        this.openAccountCommandHandler = new OpenAccountCommandHandler(openAccountService);
        this.bookCreditCommandHandler = new BookCreditCommandHandler(bookingService);
        this.bookDebitCommandHandler = new BookDebitCommandHandler(bookingService);
        this.creditInstalmentsCommandHandler = new CreditInstalmentsCommandHandler(bookingService);
        this.authorizeCommandHandler = new AuthorizeCommandHandler(authorizationService);
        this.settleAuthorizationCommandHandler = new SettleAuthorizationCommandHandler(authorizationService);
        this.reverseTransactionCommandHandler = new ReverseTransactionCommandHandler(reversalService);
        this.endOfDayCommandHandler = new EndOfDayCommandHandler(endOfDayService);
        this.reconcileFromDayCommandHandler = new ReconcileFromDayCommandHandler(endOfDayService);
        this.capitalizeInterestCommandHandler = new CapitalizeInterestCommandHandler(endOfDayService);
        this.endOfDayBatchCommandHandler = new EndOfDayBatchCommandHandler(endOfDayService);
        commandBus.register(OpenAccountCommand.class, openAccountCommandHandler);
        commandBus.register(BookCreditCommand.class, bookCreditCommandHandler);
        commandBus.register(BookDebitCommand.class, bookDebitCommandHandler);
        commandBus.register(CreditInstalmentsCommand.class, creditInstalmentsCommandHandler);
        commandBus.register(AuthorizeCommand.class, authorizeCommandHandler);
        commandBus.register(SettleAuthorizationCommand.class, settleAuthorizationCommandHandler);
        commandBus.register(ReverseTransactionCommand.class, reverseTransactionCommandHandler);
        commandBus.register(EndOfDayCommand.class, endOfDayCommandHandler);
        commandBus.register(ReconcileFromDayCommand.class, reconcileFromDayCommandHandler);
        commandBus.register(CapitalizeInterestCommand.class, capitalizeInterestCommandHandler);
        commandBus.register(EndOfDayBatchCommand.class, endOfDayBatchCommandHandler);
    }

    public Optional<Account> account(String accountId) {
        return accountRepository.findById(accountId);
    }

    public List<Object> commandHandlers() {
        return List.of(
                openAccountCommandHandler,
                bookCreditCommandHandler,
                bookDebitCommandHandler,
                creditInstalmentsCommandHandler,
                authorizeCommandHandler,
                settleAuthorizationCommandHandler,
                reverseTransactionCommandHandler,
                endOfDayCommandHandler,
                reconcileFromDayCommandHandler,
                capitalizeInterestCommandHandler,
                endOfDayBatchCommandHandler
        );
    }
}
