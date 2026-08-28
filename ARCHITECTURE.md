# [ARCHITECTURE.md](http://ARCHITECTURE.md)

## 1. Overview

The solution uses an event-sourced account model.

The supplied E1–E10 records are treated internally as **commands** because they represent incoming requests that may still require validation or may be rejected.

Successful or rejected decisions produce immutable **domain events**.

Those domain events are stored in an append-only event store and form the source of truth for the account.

The main architectural flow is:

```text
External Command
      |
      v
Account Aggregate
validate / decide
      |
      v
Domain Event(s)
      |
      v
Event Store
      |
      +-------------------+
      |                   |
      v                   v
Account Projection     Ledger Projection
      |                   |
      v                   v
Fast operational       Monetary history
reads
```

The implementation remains in-memory for the exercise, but the design preserves the same boundaries that could be used with a persistent event store.

No database, web layer, UI, or persistence layer is introduced.

---



## 2. Why event sourcing was chosen

I considered two main designs.

The first was a traditional append-only monetary ledger combined with separately maintained authorization state.

The second was event sourcing, where immutable domain events represent every accepted or rejected business decision and all current state is derived from those events.

I chose event sourcing because several requirements naturally depend on reconstructing historical state.

The specification includes:

```text
ordered input
append-only history
no mutation or deletion
reversals
backdated value dates
authorization holds
settlements
historical fee reconciliation
```

The most important reason is the interaction between backdated events and historical state.

For example, E7 arrives on Day 5 but has:

```text
value_date = Day 2
```

Later, E9 reverses E7 with the same historical value date.

That can require recalculating the account from Day 2 forward, including previously assessed overdraft fees and potentially interest accruals.

If the system only stored today's balance and today's authorization state, it would lose information needed to reconstruct what happened historically.

With event sourcing, the system retains facts such as:

```text
CreditBooked
DebitBooked
AuthorizationApproved
AuthorizationDeclined
SettlementBooked
SettlementRejected
TransactionReversed
OverdraftFeeAssessed
OverdraftFeeReversed
DailyInterestAccrued
InterestCapitalized
```

The current account state can always be reconstructed by replaying those events.

This gives three important properties.

### Auditability

The original history is never destroyed.

A reversal does not delete the original debit.

A fee reversal does not delete the original fee.

### Historical reconstruction

The account can be reconstructed at an earlier point in the event stream.

### Deterministic replay

Given the same ordered domain events, the same account state and projections should be produced.

---



## 3. Trade-off of event sourcing

Event sourcing has a cost.

The main one is read amplification.

If an account has hundreds of thousands of events, rebuilding its current state from event 1 for every authorization request would be inefficient.

For that reason, the production-oriented design includes a derived:

```text
AccountSnapshot
```

The event store remains the source of truth.

The snapshot exists only to make current-state reads fast.

Conceptually:

```text
Event Store
    |
    | replay/update
    v
AccountSnapshot
```

The snapshot contains the information required for common operational decisions such as authorization.

It can always be rebuilt from the event stream if necessary.

For this six-day exercise the stream is tiny, so replay cost is not a practical problem, but the projection makes the architectural trade-off explicit.

---



## 4. Commands versus events

Although the exercise refers to E1–E10 as an event stream, I model them internally as **commands**.

The reason is that some of them still require business validation.

For example:

```text
SETTLEMENT Auth-Z AED 180
```

cannot automatically be considered an accepted financial fact.

The system must first determine whether Auth-Z exists.

Therefore the incoming object is conceptually a command such as:

```text
SettleAuthorizationCommand
```

The aggregate evaluates the command and may produce:

```text
SettlementBooked
```

or:

```text
SettlementRejected
```

This distinction keeps the authoritative event store limited to facts that the system has actually decided occurred.

A command describes something the outside world is asking the system to do.

A domain event describes something the system has decided actually happened.

---



## 5. Command processing flow

The general command flow is:

```text
1. Receive command.
2. Load current account state.
3. Validate business rules.
4. Produce domain event(s).
5. Append events using optimistic concurrency.
6. Apply events to the account projection.
7. Update derived projections such as the monetary ledger.
```

For example:

```text
AuthorizeCommand(Auth-A, AED 200)

        |
        v

Check available balance

        |
        v

AuthorizationApproved(Auth-A, AED 200)
```

or:

```text
AuthorizationDeclined(Auth-A, reason)
```

---



## 6. Event store as the source of truth

The event store contains immutable domain events.

No domain event is mutated or deleted.

Conceptually, each stored event contains:

```java
public record StoredEvent(
    String eventId,
    String accountId,
    long version,
    DomainEvent event
) {}
```

The important ordering is:

```text
accountId + version
```

Each account has its own monotonically increasing stream version.

For example:

```text
ACC-001 / version 1
ACC-001 / version 2
ACC-001 / version 3

ACC-002 / version 1
ACC-002 / version 2
```

---



## 7. Why account stream version exists

The account stream version provides both ordering and optimistic concurrency.

Suppose two authorization requests both read:

```text
ACC-001
version = 10
```

Both decide they can reserve AED 80.

The first request appends:

```text
ACC-001 / version 11
```

successfully.

The second request also tries to append version 11.

That append must fail because version 11 already exists.

The second authorization must then reload the latest account state and reevaluate its decision.

This prevents two requests from being accepted using the same stale account state.

---



## 8. Event store uniqueness constraint

A persistent implementation would enforce:

```text
UNIQUE(account_id, version)
```

This is the core optimistic concurrency guarantee.

A globally unique event identifier would also normally be enforced:

```text
UNIQUE(event_id)
```

Conceptually:

```text
event_id
account_id
version
event_type
payload
value_date
occurred_at

UNIQUE(event_id)
UNIQUE(account_id, version)
```

For this exercise the event store is in-memory, but it follows the same logical rule.

---



## 9. Account

The account contains stable information:

```java
public record Account(
    String accountId,
    Currency currency,
    long openingBalanceInMinorUnits
) {}
```

The supplied accounts are:

```text
ACC-001 — AED
ACC-002 — BHD
```

These identifiers are used directly.

No additional account identifier is introduced.

---



## 10. Currency

The supported currency precision is defined by the specification:

```text
AED = 2 decimal places
BHD = 3 decimal places
```

Conceptually:

```java
public enum Currency {
    AED(2),
    BHD(3);

    private final int scale;

    Currency(int scale) {
        this.scale = scale;
    }

    public int scale() {
        return scale;
    }
}
```

---



## 11. Monetary representation

Monetary amounts are stored as integer minor units using `long`.

I deliberately do not use `float` or `double`.

Examples:

```text
AED 1,200.00 -> 120000
AED   950.00 ->  95000
AED    25.00 ->   2500

BHD    10.000 -> 10000
BHD     3.334 ->  3334
```

This is a fixed-point representation.

The decimal position is determined by the currency.

---



## 12. Meaning of minor units

The word `minor` refers to the smallest supported monetary unit.

For AED:

```text
1 AED = 100 fils
```

Therefore:

```text
AED 12.34
=
1234 minor units
```

For BHD:

```text
1 BHD = 1000 fils
```

Therefore:

```text
BHD 10.000
=
10000 minor units
```

Fields are deliberately named explicitly:

```java
amountInMinorUnits
openingBalanceInMinorUnits
activeHoldInMinorUnits
```

rather than simply:

```java
amount
```

because the integer alone does not communicate its scale.

For example:

```text
1234 minor units
```

means:

```text
AED 12.34
```

for AED, but:

```text
BHD 1.234
```

for BHD.

---



## 13. Why fixed-point integers

Booked monetary addition and subtraction become exact integer operations.

For example:

```text
120000 - 95000
=
25000
=
AED 250.00
```

There is no binary floating-point rounding error.

This also makes monetary conservation easier to verify.

For E10:

```text
BHD 10.000
=
10000 minor units
```

Any division residual is explicit rather than hidden in decimal representation.

Fractional calculations such as interest still require explicit division and rounding, but once an amount becomes a booked monetary value it is represented exactly in minor units.

---



## 14. Domain events

Domain events represent facts accepted by the account aggregate.

Examples include:

```text
CreditBooked
DebitBooked

AuthorizationApproved
AuthorizationDeclined

SettlementBooked
SettlementRejected

TransactionReversed

OverdraftFeeAssessed
OverdraftFeeReversed

DailyInterestAccrued
InterestCapitalized
```

These events are immutable.

They are the authoritative account history.

---



## 15. AccountSnapshot

The account snapshot is a projection of the event stream.

It exists to make current account operations efficient.

Conceptually:

```java
public record AccountSnapshot(
    String accountId,
    long ledgerBalanceInMinorUnits,
    long activeHoldInMinorUnits,
    Map<String, Authorization> authorizations,
    long streamVersion
) {}
```

The snapshot is not the source of truth.

It represents:

```text
the result of applying domain events
through streamVersion
```

If the snapshot is lost or corrupted, it can be rebuilt by replaying events.

---



## 16. Why the snapshot contains streamVersion

The projection must identify exactly which events it represents.

For example:

```text
AccountSnapshot
streamVersion = 20
```

means:

```text
events 1 through 20 have been applied
```

If the event store has already reached:

```text
version 22
```

the snapshot is stale.

Before making a critical decision, the projection must either:

```text
apply events 21 and 22
```

or rebuild from the event stream.

---



## 17. Available balance

The authorization rule uses:

```text
available balance
=
ledger balance
-
active holds
```

For example:

```text
ledger balance = AED 250
active holds   = AED 200
available      = AED  50
```

The snapshot provides both values required for a fast calculation.

---



## 18. Authorization processing

An authorization command is evaluated against the latest account state.

Conceptually:

```text
1. Load AccountSnapshot at version N.
2. Ensure it is current.
3. Calculate available balance.
4. Apply requested hold.
5. Produce AuthorizationApproved or AuthorizationDeclined.
6. Append event using expected stream version N.
```

For E3:

```text
ledger balance = AED 250
active holds   = AED   0
requested      = AED 200
```

Therefore:

```text
available after hold
=
250 - 200
=
AED 50
```

Auth-A is approved.

---



## 19. Concurrent authorization example

Assume:

```text
ledger balance = AED 100
active holds   = AED   0
stream version = 10
```

Two requests arrive:

```text
Auth-X = AED 80
Auth-Y = AED 80
```

Both initially read version 10.

Request X appends:

```text
AuthorizationApproved(Auth-X)
version 11
```

Request Y attempts to append another version 11 event.

The event store rejects it because:

```text
(accountId, version)
```

must be unique.

Request Y reloads the account.

The updated state now has:

```text
active hold = AED 80
available   = AED 20
```

so Auth-Y is declined.

This prevents overspending without requiring a global lock.

---



## 20. Authorization state

Authorization information is derived from events.

Conceptually:

```java
public record Authorization(
    String authorizationId,
    long amountInMinorUnits,
    AuthorizationStatus status
) {}
```

Possible statuses include:

```java
APPROVED
DECLINED
SETTLED
```

When:

```text
AuthorizationApproved(Auth-A, 200)
```

is applied:

```text
activeHold += 200
Auth-A = APPROVED
```

When the settlement event is applied:

```text
activeHold -= 200
Auth-A = SETTLED
```

---



## 21. Monetary ledger projection

The monetary ledger is a projection of domain events that actually move booked money.

It contains entries such as:

```text
CREDIT
DEBIT
SETTLEMENT
REVERSAL
OVERDRAFT_FEE
FEE_REVERSAL
INTEREST_CAPITALIZATION
```

Authorization events do not create monetary ledger entries because holds do not affect ledger balance.

---



## 22. Ledger entry

Conceptually:

```java
public record LedgerEntry(
    String entryId,
    String accountId,
    int valueDay,
    long signedAmountInMinorUnits,
    LedgerEntryType type,
    String sourceEventId,
    String referenceId
) {}
```

Positive values increase ledger balance.

Negative values decrease ledger balance.

Examples:

```text
CreditBooked              +120000
DebitBooked                -95000
SettlementBooked           -18500
TransactionReversed        +62000
OverdraftFeeAssessed        -2500
OverdraftFeeReversed        +2500
InterestCapitalized            +...
```

---



## 23. Processing order versus value date

Domain-event order and financial value date are different concepts.

Event stream order tells us:

```text
when the system accepted the fact
```

`value_date` tells us:

```text
which financial day the posting belongs to
```

E7 demonstrates the distinction:

```text
received Day 5
value_date Day 2
```

The event remains later in the stream but contributes to Day 2 ledger balance.

The event store must never be sorted by value date.

---



## 24. Ledger balance calculation

Ledger balance for account A on day D is:

```text
opening balance
+
all monetary ledger entries
whose value_date <= D
```

Conceptually:

```text
ledgerBalance(account, day)
=
openingBalance
+
Σ(entries where valueDate <= day)
```

This supports backdated transactions naturally.

---



## 25. Backdated debit E7

Before E7:

```text
E1 +AED 1,200
E2 -AED   950
--------------
     AED   250
```

E7 later arrives:

```text
-AED 620
value_date Day 2
```

Day 2 becomes:

```text
250 - 620
=
AED -370
```

even though the debit was received on Day 5.

Because later daily balances include earlier value-dated postings, Day 3, Day 4, and later balances may also change.

---



## 26. Settlement

E5 settles Auth-A for:

```text
AED 185
```

Auth-A originally reserved:

```text
AED 200
```

The aggregate verifies that Auth-A exists and is approved.

It then emits a settlement event.

Applying that event:

```text
books -AED 185
marks Auth-A SETTLED
releases the full AED 200 hold
```

The unused AED 15 is no longer reserved.

---



## 27. Unknown settlement

E6 references:

```text
Auth-Z
```

No valid authorization exists.

Therefore the command does not emit a monetary settlement event.

Instead it produces a rejection/error event such as:

```text
SettlementRejected(
    authorizationId = Auth-Z,
    reason = UNKNOWN_AUTHORIZATION
)
```

No funds leave the account.

---



## 28. Reversal

E9 reverses E7.

The original event is never removed.

Instead the aggregate emits:

```text
TransactionReversed
```

which creates the opposite ledger effect:

```text
E7  -AED 620
E9  +AED 620
```

The audit history therefore contains both the original transaction and its reversal.

---



## 29. End-of-day processing

Daily overdraft assessment and daily interest accrual are triggered through explicit commands rather than being embedded inside individual transaction handlers.

In a production system, a scheduler or cron job could trigger:

```text
EndOfDayBatchCommand(day)
```

The batch process then iterates over all accounts and sends:

```text
EndOfDayCommand(accountId, day)
```

for each one.

Conceptually:

```text
Scheduler
    |
    v
EndOfDayBatchCommand(day)
    |
    +----> EndOfDayCommand(ACC-001, day)
    |
    +----> EndOfDayCommand(ACC-002, day)
    |
    +----> ...
```

The batch command itself is orchestration.

Each account independently decides what domain events, if any, should be emitted.

---



## 30. EndOfDayCommand

Conceptually:

```java
public record EndOfDayCommand(
    String accountId,
    int day
) {}
```

The account aggregate evaluates:

```text
closing ledger balance
existing overdraft-fee state
daily interest eligibility
```

The command itself does not automatically create a domain event.

A command asks the domain to evaluate a condition.

An event represents a fact that resulted from that evaluation.

For example:

```text
EndOfDayCommand(ACC-001, Day 2)

        |
        v

calculate closing ledger balance

        |
        +---- negative
        |       |
        |       v
        | OverdraftFeeAssessed
        |
        +---- positive
                |
                v
          DailyInterestAccrued
```

If the closing balance is zero, neither a fee nor interest needs to be emitted.

---



## 31. Overdraft fee

The overdraft fee is:

```text
AED 25.00
```

or:

```text
2500 minor units
```

A fee is required once per account/day when that day's closing ledger balance is negative.

A fee assessment produces:

```text
OverdraftFeeAssessed
```

with:

```text
value_date = assessed day
```

The corresponding monetary projection becomes:

```text
-AED 25
```

---



## 32. End-of-day overdraft assessment

For every account, `EndOfDayCommand` calculates the closing ledger balance using:

```text
opening balance
+
all booked monetary effects
with value_date <= evaluated day
```

If:

```text
closing balance < 0
```

and there is no effective fee already present for that account/day, the aggregate emits:

```text
OverdraftFeeAssessed
```

The event is appended to the same account stream using optimistic concurrency.

---



## 33. Historical fee calculation

Backdated postings require historical fee reevaluation.

Suppose E7 makes Day 2:

```text
AED -370
```

A Day 2 overdraft fee is then applied:

```text
AED -25
```

making Day 2:

```text
AED -395
```

That fee also affects later balances.

For example:

```text
Day 3:
-395 + 400
=
AED 5
```

Then the Day 4 settlement:

```text
5 - 185
=
AED -180
```

can create another overdraft condition.

Historical fee calculation is therefore performed chronologically from the affected value date forward.

---



## 34. Fee reconciliation after a backdated reversal

E9 can remove the condition that caused previous overdraft fees.

Because the event store contains the full history, the system can reconstruct the affected historical account path.

The reconciliation process is:

```text
1. Append TransactionReversed.
2. Recalculate historical closing balances from the reversal value date.
3. Recompute which overdraft fees should exist.
4. Compare expected fee state with previously emitted fee events.
5. Emit OverdraftFeeReversed for fees no longer justified.
6. Emit new OverdraftFeeAssessed events for newly justified days.
7. Continue until the historical fee state stabilizes.
```

No existing event is removed.

---



## 35. Fee reversal

Suppose the stream contains:

```text
DebitBooked             -620
OverdraftFeeAssessed     -25
```

and the debit is later reversed.

If the recomputed historical balance shows the fee is no longer required, the system emits:

```text
OverdraftFeeReversed     +25
```

The event history becomes:

```text
DebitBooked             -620
OverdraftFeeAssessed     -25
TransactionReversed     +620
OverdraftFeeReversed     +25
```

This preserves both immutable history and the corrected economic outcome.

---



## 36. Why full event history matters during reconciliation

Fee reconciliation is one reason the event-sourced model is useful.

Historical reconstruction can require more than today's ledger balance.

The system may need to know:

```text
which debits existed
which reversals existed
which settlements existed
which authorizations were approved
which holds were active
which fee events had already been emitted
```

The event stream preserves all of these facts.

A mutable current account object alone would not.

---



## 37. Daily interest accrual

The same end-of-day evaluation determines whether the account earns interest.

Daily interest is:

```text
0.04%
```

which equals:

```text
4 / 10000
```

Interest applies only when:

```text
closing ledger balance > 0
```

With integer minor units:

```text
interest
=
balanceInMinorUnits * 4 / 10000
```

with explicit rounding when the result does not equal a complete minor unit.

If the rounded daily interest is positive, the aggregate emits:

```text
DailyInterestAccrued
```

This event records the rounded accrual but does not yet change ledger balance.

---



## 38. Daily interest example

For:

```text
AED 250.00
```

the balance is:

```text
25000 minor units
```

Interest:

```text
25000 * 4 / 10000
=
10 minor units
```

Therefore:

```text
AED 0.10
```

is accrued for that day.

Each day's accrual is independently rounded to the currency precision.

---



## 39. Interest capitalization on Day 6

After the final Day 6 accrual has been calculated, the system issues:

```text
CapitalizeInterestCommand(accountId)
```

The aggregate obtains the effective daily interest accruals for Days 1 through 6 and calculates:

```text
capitalized total
=
sum of rounded daily accruals
```

It then emits:

```text
InterestCapitalized
```

This creates the single required ledger credit at the end of Day 6.

By construction:

```text
capitalized interest
=
sum of rounded daily accruals
```

No remainder is discarded.

---



## 40. Backdated changes and interest

A backdated transaction can also change historical positive balances and therefore change previously calculated interest accruals.

Historical reconciliation must therefore consider:

```text
daily closing balance
overdraft fee state
daily interest state
```

for every affected day.

Because domain events are immutable, previously emitted accrual events are never rewritten.

If an accrual needs correction, the correction must be represented explicitly or the effective accrual must be derived from the event stream in a way that preserves the final invariant.

For this exercise, the key invariant is:

```text
the final effective rounded accrual for each day
must reflect the corrected historical closing balance
```

before interest is capitalized on Day 6.

---



## 41. Idempotency of end-of-day processing

A scheduler may retry an end-of-day operation.

Therefore `EndOfDayCommand` must be idempotent.

Before emitting:

```text
OverdraftFeeAssessed
```

the aggregate checks whether an effective fee already exists for that account/day.

Before emitting:

```text
DailyInterestAccrued
```

the aggregate checks whether that day's accrual has already been recorded for the current effective history.

The event stream is the source of truth for determining whether an end-of-day action has already occurred.

---



## 42. Backdated events after end-of-day processing

An end-of-day calculation is not necessarily permanently final because later commands may have earlier value dates.

E7 demonstrates this:

```text
received Day 5
value_date Day 2
```

If Day 2 has already been processed, E7 changes its historical closing balance.

Therefore a backdated monetary command triggers historical reconciliation from its value date forward.

Conceptually:

```text
backdated command
      |
      v
ReconcileFromDayCommand(accountId, affectedDay)
```

The reconciliation reevaluates:

```text
daily balances
overdraft fees
interest accruals
```

for the affected day and later days in the six-day window.

---



## 43. Take-home scheduling implementation

The exercise does not require a real scheduler or cron infrastructure.

`ReplayMain` simulates production behavior by issuing the same commands at the appropriate points in the six-day replay.

Conceptually:

```text
ReplayMain

process Day 1 commands
↓
EndOfDayCommand for every account

process Day 2 commands
↓
EndOfDayCommand for every account

...

process Day 6 commands
↓
EndOfDayCommand for every account
↓
CapitalizeInterestCommand for every account
```

This keeps scheduling infrastructure out of scope while preserving the domain boundary that would exist in production.

---



## 44. E10 instalments

E10 credits:

```text
BHD 10.000
```

as three instalments.

Using minor units:

```text
10000 / 3
=
3333 remainder 1
```

The residual is allocated deterministically:

```text
3334
3333
3333
```

which represents:

```text
BHD 3.334
BHD 3.333
BHD 3.333
```

and totals exactly:

```text
BHD 10.000
```

No money is created or discarded.

---



## 45. E10 value date

Only one value date is supplied:

```text
Day 5
```

No instalment schedule is provided.

Therefore all three generated monetary postings use:

```text
value_date = Day 5
```

No additional dates are invented.

---



## 46. Daily reporting projection

The task requires reporting:

```text
per day
per account
```

including:

```text
closing ledger balance
fee assessments
authorization states
errors
```

This is represented as a derived projection.

Conceptually:

```java
public record DailySnapshot(
    String accountId,
    int day,
    long ledgerBalanceInMinorUnits,
    long activeHoldInMinorUnits,
    long availableBalanceInMinorUnits,
    List<FeeAssessment> fees,
    Map<String, AuthorizationStatus> authorizations,
    List<LedgerError> errors
) {}
```

This snapshot is not authoritative.

It can be regenerated from domain events.

---



## 47. Snapshot versus event store

The distinction is:

```text
Event Store
=
source of truth

AccountSnapshot
=
fast current projection

Ledger
=
monetary projection

DailySnapshot
=
reporting projection
```

A projection may be updated or rebuilt.

Domain events are never rewritten.

---



## 48. Projection recovery

If a snapshot becomes unavailable or inconsistent, it can be rebuilt.

Conceptually:

```text
empty AccountSnapshot
       +
replay account events
       =
rebuilt AccountSnapshot
```

The same principle applies to:

```text
ledger projection
daily reporting projection
```

This rebuildability is one of the main benefits of the chosen architecture.

---



## 49. Concurrency model

Concurrency is handled per account stream.

Different accounts can be processed independently:

```text
ACC-001
ACC-002
```

Operations against the same account use optimistic concurrency through stream versioning.

This avoids a global lock while ensuring two account decisions cannot both commit against the same stream version.

---



## 50. Production read performance

A production account could have a very large event stream.

Reading every historical event for every authorization would be inefficient.

Therefore the hot authorization path would normally use:

```text
AccountSnapshot
```

for current state.

If the snapshot represents version N and the stream is at version N, it can be used immediately.

If newer events exist, the snapshot must first catch up before the business decision is made.

---



## 51. Production write consistency

The critical guarantee is:

```text
read state at version N
+
make business decision
+
append only if stream is still version N
```

If the stream changed meanwhile, the operation is retried.

This is particularly important for authorization because available balance must not be evaluated against stale state.

---



## 52. Why not a traditional mutable account row

A simple model such as:

```text
accounts.balance
accounts.active_hold
```

would make current reads easy but would make historical corrections and reconstruction harder.

The requirements explicitly include:

```text
backdated transactions
reversals
append-only history
historical fee recalculation
```

Those requirements benefit from retaining the full sequence of immutable account decisions.

For this task, that benefit outweighs the additional event-sourcing complexity.

---



## 53. Complexity trade-off

Event sourcing adds:

```text
event versioning
projection maintenance
replay logic
optimistic concurrency
historical reconciliation
```

that a simple mutable model would not require.

The benefit is:

```text
auditability
historical reconstruction
deterministic replay
safe reversals
projection rebuildability
```

For this particular ledger problem, those benefits align closely with the requirements.

---



## 54. Main invariants



### Event immutability

Stored domain events are never mutated or deleted.

### Stream ordering

Each account event stream has one deterministic version sequence.

### Optimistic concurrency

Only one event may occupy a given:

```text
(accountId, version)
```

pair.

### Monetary precision

Booked money is stored in exact integer minor units.

### Authorization

An approved authorization must not make available balance negative.

### Hold behavior

Authorization holds reduce available balance but not ledger balance.

### Settlement

A settlement must reference a valid approved authorization.

### Reversal

A reversal creates a compensating event instead of deleting the original transaction.

### Overdraft fee

At most one effective overdraft fee applies per account/day.

### Fee reconciliation

Backdated corrections can generate compensating fee-reversal events.

### Interest

Capitalized interest equals exactly the sum of final effective rounded daily accruals.

### Instalments

Generated instalments must conserve the original monetary amount exactly.

---



## 55. Final architecture

```text
                         External Commands
                                |
                                v
                       +------------------+
                       | Account Aggregate|
                       | validate / decide|
                       +------------------+
                                |
                                v
                       +------------------+
                       |   Domain Events  |
                       +------------------+
                                |
                                v
                       +------------------+
                       |    Event Store   |
                       | account/version  |
                       +------------------+
                          /       |       \
                         /        |        \
                        v         v         v
              +-------------+ +--------+ +--------------+
              | Account     | | Ledger | | Daily        |
              | Snapshot    | | View   | | Reporting    |
              +-------------+ +--------+ +--------------+
                    |
                    v
          available balance /
          authorization decisions
```

The central architectural rule is:

```text
Domain events are truth.
Everything else is a projection.
```

The account snapshot exists for fast operational reads.

The monetary ledger exists for financial reporting and balance calculations.

Daily snapshots exist for the required replay output.

All of them can be rebuilt from the immutable account event stream.

---



## 56. Design philosophy

The design prioritizes:

```text
correctness
auditability
historical reconstruction
monetary conservation
safe concurrency
explicit value-date semantics
clear trade-offs
```

The implementation remains intentionally small.

The exercise does not require:

```text
Spring
database persistence
Kafka
distributed projections
external caches
web APIs
real cron infrastructure
```

so those are not introduced.

The event-sourcing pattern is used because it directly supports the hardest requirements of the exercise, not because event sourcing is assumed to be universally better than a traditional ledger implementation.