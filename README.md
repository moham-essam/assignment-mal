# assignment-mal

In-memory value-dated banking ledger. Java 21, Maven, JUnit 5. No database, web layer, or Spring.

## Run the assignment replay

```bash
mvn -q exec:java
```

That opens ACC-001 / ACC-002, dispatches E1–E10 with catch-up end-of-day and late-arrival reconcile, capitalizes day 6, and prints closings, interest, fees, and command failures.

## Tests

```bash
mvn -q test
```

The tagged `known-limitation` test (backdated posting after day-6 capitalization) is excluded from the default Surefire run.
