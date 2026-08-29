# WORKLOG.md

High-level log reconstructed from Git author timestamps (`UTC+4` / GST) and the files those commits introduced. Not a minute-by-minute diary.

**Submission cutoff:** 2026-08-28 (last commit that contains ledger logic and the rewritten architecture docs). Everything after that date is fixture-value correction, then these missing documents. Core command/ledger behaviour is unchanged.

---

## 2026-08-28

### 08:19 GST

Initial repository: `.gitignore` and a one-line `README.md`.

### 09:03 GST

Added `ARCHITECTURE.md`, `AMBIGUITIES.md`, and `REJECTED.md`.

The first architecture draft specified an event-sourced account: commands in, immutable domain events in an event store, projections out. Ambiguities and rejected criteria were already written against the assignment arithmetic (fees after E7, E10 thirds, interest remainder).

### 12:20 GST

Implemented account open, credit, debit, authorization, and settlement on an in-memory `Account` aggregate, unit of work, and command bus (`Add open, credit, debit, authorize, and settle on an in-memory ledger.`).

This is already the append-only journal + snapshot model (`LedgerEntry` `putIfAbsent`, `InMemoryUnitOfWork` CAS). There is no event store and no `apply(event)` in this commit. `application.yaml` lands here with the shipped numeric constants.

### 14:36 GST

Added end-of-day, backdated reconcile, reversal, instalments, capitalization, `SimulationEngine` / `ScenarioFixture`, `ReplayMain`, and the assignment tests (`Add EOD, reverse, reconcile, and assignment replay.`).

`ARCHITECTURE.md` is rewritten away from event sourcing (“Why not event sourcing”: E7/E9/fees/interest are value-day closing sums over the journal). `BackdatedAfterCapitalizationTest` is tagged `known-limitation` and excluded in Surefire.

### 14:56 GST

Documentation aligned with the implementation (`Document ledger decisions and stale posting-time balances.`). Architecture, ambiguities, and rejected criteria now describe the append-only value-dated journal. `LedgerConsistencyTest` records that row `balanceBefore` / `balanceAfter` are posting-order and go stale after a later same-or-earlier `valueDay`.

---

## 2026-08-29 (after submission)

No core ledger, policy, or handler changes. Scenario fixture only, then missing docs, then a test aligned to the fixture.

### 09:10 GST

`Fix E10 day` — `ScenarioFixture`: E10 arrives on day 5 (still `valueDay = 5`); E9 stays on day 6. Dispatch order becomes E10 before E9. No split/fee/interest code changed.

### 09:12 GST

`Fix E6 E8 values` — `ScenarioFixture`: E6 amount `18500` → `18000`; E8 amount `90000` → `90_00L` (9000). Same commands, same handlers.

### 10:42 GST

`ScenarioAcceptanceTest.assignmentStreamIsE1ThroughE10InOrder` still required E9 before E10. After `Fix E10 day` the engine dispatches E10 on day 5 (`E8` then `E10` then `EOD-D5` then `E9`). Assertions updated to that order. `mvn test`: 83 passed. No fixture or ledger change.

### Documents

`NUMBERS.md` and `WORKLOG.md` added. They were not in the 2026-08-28 tree. Application code is not modified in this step.

---

This worklog is reconstructed from the intact Git history and the current repository files. It is not an invented retrospective. Post-submission Git activity is the two fixture corrections, this file and `NUMBERS.md`, then the acceptance-test order fix.
