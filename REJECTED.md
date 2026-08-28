# REJECTED.md

This document records acceptance criteria that I intentionally do not implement as written, together with approaches considered during the design and later abandoned.

The purpose is to make conflicting requirements and design trade-offs explicit rather than silently choosing one interpretation.

---

## Rejected acceptance criteria

### 1. "E7 causes exactly one overdraft fee to be assessed, on Day 2."

**Rejected as written.**

E7 is received on Day 5 but has:

```text id="xnd75b"
value_date = Day 2
amount = AED -620.00
```

Before E7, the Day 2 ledger balance is:

```text id="x51j2e"
AED 1,200.00
- AED 950.00
= AED 250.00
```

After E7:

```text id="c6p8m1"
AED 250.00
- AED 620.00
= AED -370.00
```

Day 2 therefore qualifies for an overdraft fee.

The fee itself is booked with Day 2 as its value date:

```text id="ccib4u"
AED -25.00
```

so the effective Day 2 balance becomes:

```text id="vb5sxt"
AED -395.00
```

E4 contributes AED 400.00 on Day 3:

```text id="b97s9h"
-395 + 400
=
AED 5.00
```

E5 then settles Auth-A for AED 185.00 on Day 4:

```text id="153bga"
5 - 185
=
AED -180.00
```

Day 4 is therefore also negative.

The non-negotiable rule says an overdraft fee is assessed once per day per account when that day's closing ledger balance is negative.

Therefore the system cannot guarantee that E7 results in exactly one fee on Day 2.

Historical fee calculation must consider the downstream effect of earlier value-dated fees on later closing balances.

---

### 2. "The three BHD instalments in E10 must each be BHD 3.334."

**Rejected.**

E10 credits:

```text id="wdhed6"
BHD 10.000
```

Three identical instalments of BHD 3.334 would total:

```text id="pb7wbu"
3.334
+ 3.334
+ 3.334
-------
10.002
```

That creates:

```text id="5ft4rb"
BHD 0.002
```

which did not exist in the original transaction.

The implementation preserves the original amount instead.

Using minor units:

```text id="56hvxl"
BHD 10.000
=
10000 minor units
```

and:

```text id="r8jyki"
10000 / 3
=
3333 remainder 1
```

The residual is assigned deterministically:

```text id="jdu4yb"
3334
3333
3333
```

which represents:

```text id="15qw3k"
BHD 3.334
BHD 3.333
BHD 3.333
```

and totals exactly:

```text id="vha8gx"
BHD 10.000
```

Preserving the original monetary total is a stronger invariant than forcing the rounded instalments to be identical.

---

### 3. "If the rounded daily interest accruals do not sum to the capitalized total, the remainder is discarded."

**Rejected.**

This directly conflicts with the non-negotiable rule:

> The rounded daily accruals must sum exactly to the capitalized total.

The implementation therefore defines capitalization as:

```text id="sb1m77"
capitalized interest
=
sum of the final rounded daily interest accruals
```

The capitalization amount is not calculated independently and then reconciled afterward.

It is derived directly from the daily rounded amounts.

Therefore there is no remainder to discard.

Discarding a difference would violate the explicit equality requirement.

---

## Criterion intentionally not rejected

### "After E9, all balances and fees return to their pre-E7 values."

I do not reject this criterion under the chosen design.

E9 reverses E7 using the same historical value date.

The system then reconciles the affected historical days.

If overdraft fees were assessed only because E7 caused historical balances to become negative, the original fee events remain in the event store and compensating:

```text id="7ls4x9"
OverdraftFeeReversed
```

events are emitted.

For example:

```text id="nsqay3"
DebitBooked               -620
OverdraftFeeAssessed       -25
TransactionReversed       +620
OverdraftFeeReversed       +25
```

The immutable history remains intact while the effective financial result can return to its pre-E7 position.

---

# Approaches considered and abandoned

## 1. Traditional ledger plus independently mutable authorization state

### Considered

Use an append-only monetary ledger as the financial source of truth while independently maintaining current authorization and hold state.

### Why it was abandoned

Backdated entries and historical reconciliation make it useful to reconstruct not only booked money but also the sequence of domain decisions.

The final design therefore uses domain events as the source of truth and derives both account state and monetary ledger views from them.

---

## 2. Treating E1–E10 directly as authoritative events

### Considered

Store the supplied E1–E10 records directly as domain events.

### Why it was abandoned

Some supplied records still require validation.

E6 is the clearest example:

```text id="chij8a"
SETTLEMENT Auth-Z
```

Auth-Z does not exist, so the settlement cannot be treated as a fact that successfully occurred.

The supplied records are therefore treated internally as commands.

Commands produce authoritative events such as:

```text id="dc70tc"
SettlementBooked
```

or:

```text id="e731lf"
SettlementRejected
```

---

## 3. Using only AtomicLong for active holds

### Considered

Maintain active hold amount using:

```java id="gddqsf"
AtomicLong activeHoldInMinorUnits;
```

### Why it was abandoned

Authorization approval is a compound business operation.

It involves:

```text id="u1v38j"
reading available balance
checking the requested hold
recording authorization identity
recording authorization status
updating active holds
```

Making one numeric field atomic does not make the complete decision atomic.

The event-sourced design instead uses account-stream optimistic concurrency.

---

## 4. Mutable current balance as the source of truth

### Considered

Maintain:

```text id="6auu22"
account.currentBalance
```

and update it whenever money moves.

### Why it was abandoned

E7 arrives on Day 5 but has:

```text id="jkhh3f"
value_date = Day 2
```

The system must later be able to recalculate Day 2 and all affected later days.

A single mutable current balance cannot represent this historical structure cleanly.

The monetary ledger projection therefore retains value-dated postings.

---

## 5. Deleting the original transaction when a reversal arrives

### Considered

Delete or replace E7 after E9 reverses it.

### Why it was abandoned

This directly conflicts with the append-only requirement.

The final design keeps both facts:

```text id="o2x7nd"
DebitBooked
TransactionReversed
```

The reversal produces an opposite financial effect without destroying history.

---

## 6. Deleting previously assessed fees after historical correction

### Considered

Remove an overdraft fee if a later backdated reversal means the fee should no longer apply.

### Why it was abandoned

Domain events are immutable and append-only.

Instead, the system emits:

```text id="zrkk47"
OverdraftFeeReversed
```

to compensate for the original:

```text id="3ig1f7"
OverdraftFeeAssessed
```

event.

This preserves both audit history and the corrected economic result.

---

## 7. BigDecimal for stored monetary amounts

### Considered

Use Java `BigDecimal` to represent all booked monetary amounts.

### Why it was abandoned

`BigDecimal` would be a valid implementation choice.

The final design instead uses integer minor units because AED and BHD have fixed precision and booked addition and subtraction can therefore be exact integer operations.

For example:

```text id="q32eu3"
AED 12.34 = 1234 minor units
BHD 1.234 = 1234 minor units
```

This also makes E10 residual allocation explicit.

The decision is a trade-off, not a claim that `BigDecimal` is unsuitable for financial systems.

---

## 8. float or double for monetary values

### Considered

Use Java floating-point primitive values for simplicity.

### Why it was abandoned

Binary floating point cannot exactly represent many decimal monetary values.

That makes exact monetary invariants harder to guarantee.

The ledger therefore stores integer minor units.

---

## 9. Three identical rounded instalments for E10

### Considered

Round each instalment independently to:

```text id="fgxsx4"
BHD 3.334
```

### Why it was abandoned

Three such instalments total BHD 10.002.

The final design allocates the one-minor-unit residual to a single instalment and preserves the exact original BHD 10.000 total.

---

## 10. Full replay for every authorization in a production design

### Considered

Rebuild the account from event 1 every time an authorization command arrives.

### Why it was abandoned

This is acceptable for the tiny exercise stream but would become expensive for high-activity production accounts.

The architecture therefore includes an `AccountSnapshot` projection containing current operational state and the last applied stream version.

The event store remains authoritative, while the snapshot provides a fast authorization read path.

---

## Decision principle

Where requirements conflict, I prioritize:

```text id="y7gv5x"
1. explicit non-negotiable rules
2. conservation of monetary value
3. immutable / append-only history
4. value-date correctness
5. deterministic replay
6. explicit rather than hidden corrections
```

A criterion or approach is rejected only where following it would violate one of these stronger invariants.
