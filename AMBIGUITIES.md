# ambiguities.md

This document records only ambiguities that materially affected the implementation.

## 1. Reversal after overdraft fees were already assessed

### Ambiguity

E7 is a backdated debit with:

```text id="m2g7pk"
value_date = Day 2
```

If E7 causes one or more historical closing balances to become negative, the system may emit `OverdraftFeeAssessed` events.

Later, E9 reverses E7 with the same historical value date.

The specification does not explicitly state whether previously assessed overdraft fees that existed only because of E7 should remain, or whether the historical fee position should be recalculated.

### Decision

The system recalculates the affected historical balance path from the reversal's `value_date` forward.

If a previously assessed fee is no longer justified, the original fee event remains in the event store and a compensating:

```text id="78gixw"
OverdraftFeeReversed
```

event is emitted.

The system then continues recalculating later days because reversing an earlier fee changes every subsequent closing balance and may make later fees unnecessary as well.

### Reasoning

The overdraft fee is justified by a day's closing ledger balance.

If a backdated reversal changes that closing balance, the condition that originally caused the fee may no longer exist.

At the same time, the event store is append-only.

Therefore the system must not delete the original fee event.

The combination:

```text id="z3ednt"
OverdraftFeeAssessed
+
OverdraftFeeReversed
```

preserves the historical audit trail while restoring the correct economic outcome.

Historical fee reconciliation runs chronologically from the affected value date forward until no additional fee or fee-reversal events are required.

---

## 2. Supplied "events" versus internal commands

### Ambiguity

The specification refers to E1–E10 as an event stream.

However, some entries still require validation before they can be treated as accepted facts.

For example:

```text id="4v5nmo"
E6
SETTLEMENT Auth-Z
```

must be rejected because Auth-Z does not exist.

Treating E6 directly as an authoritative settlement event would incorrectly imply that the settlement succeeded.

### Decision

The supplied E1–E10 records are treated internally as **commands**.

The account aggregate evaluates each command and emits authoritative domain events such as:

```text id="x3j3bt"
CreditBooked
DebitBooked
AuthorizationApproved
AuthorizationDeclined
SettlementBooked
SettlementRejected
TransactionReversed
```

These emitted domain events are stored in the event store and form the source of truth.

### Reasoning

A command describes something the outside world is asking the system to do.

A domain event describes something the system has decided actually happened.

This distinction allows invalid operations such as E6 to remain visible as rejected decisions without incorrectly creating financial postings.

---

## 3. E10 instalment dates

### Ambiguity

E10 is described as:

```text id="mxjs6g"
CREDIT ACC-002 BHD 10.000
posted as three equal instalments
value_date Day 5
```

The specification requires three instalments but supplies only one value date and no instalment schedule.

### Decision

All three booked credit events use:

```text id="ks3e64"
value_date = Day 5
```

### Reasoning

There is no information that justifies assigning different dates to the instalments.

Using Day 5 for all three preserves the supplied financial effective date and avoids inventing future dates.
