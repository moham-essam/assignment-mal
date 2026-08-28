# Ambiguities

This document records ambiguities that materially affected the implementation, and the decisions taken.

---

## 1. Reversal after overdraft fees were already assessed

### Ambiguity

E7 is a backdated debit (`valueDay` = Day 2). If it makes one or more historical closings negative, the ledger books `OVERDRAFT_FEE` rows on those days.

E9 later reverses E7 on the same value day.

The specification does not say whether fees that existed only because of E7 should remain, or whether the historical fee position should be recalculated.

### Decision

After the reversal commits, `ReconcileFromDayCommand` walks from the original `valueDay` through the last closed day.

If a previously booked fee is no longer justified, the original `OVERDRAFT_FEE` row stays and a compensating `FEE_REVERSAL` is appended. Later days are walked as well, because reversing an earlier fee changes every subsequent close.

### Reasoning

The overdraft fee is justified by that day’s postings-only closing. A backdated reversal can remove that condition. The journal is append-only, so the original fee row is not deleted. `OVERDRAFT_FEE` + `FEE_REVERSAL` keeps the audit trail and restores the economic outcome.

---

## 2. Supplied “events” versus commands

### Ambiguity

The specification presents E1–E10 as an event stream. Some of those records still need validation before they can be treated as accepted facts. E6 (`SETTLEMENT Auth-Z`) must be rejected because Auth-Z does not exist. Treating E6 as an already-true settlement would book money that never should have moved.

### Decision

E1–E10 are **commands**. The account evaluates each command and either appends ledger / authorization rows or returns a `CommandResult` failure (`UNKNOWN_AUTHORIZATION`, `INSUFFICIENT_AVAILABLE_BALANCE`, `NOT_REVERSIBLE`, …). The append-only ledger is the financial source of truth. Rejected commands do not create postings.

### Reasoning

A command is a request. A ledger row is something the system accepted. That split keeps E6 visible as a declined decision without creating a settlement.

---

## 3. Should authorize check available balance before committing?

### Ambiguity

The specification does not say whether an authorization must succeed only when available funds cover the hold, or whether a hold may be approved into a negative available position.

### Decision

**Yes.** Authorize calls `ensureSufficientAvailable` before commit. Available is `amountInMinorUnits - holdAmountInMinorUnits`. If available is too low, the service still commits a `DECLINED` authorization (no hold) and returns `INSUFFICIENT_AVAILABLE_BALANCE` (E8).

### Reasoning

A hold reserves spendable funds. Approving a hold the customer cannot cover would let later settlements or debits fight over the same money. Recording `DECLINED` keeps the attempt auditable without applying the hold.

---

## 4. May a debit take the booked amount below zero?

### Ambiguity

E7 is AED −620.00 on Day 2 against a Day 2 close of AED 250.00. The specification can be read as “reject the debit” or “book it and treat the overdraft as a closing-balance problem.”

### Decision

**Debit is allowed to go negative.** `BookDebitCommand` does not call `ensureSufficientAvailable`. E7 books. Overdraft fees and interest are assessed from the resulting closings, and E9 can reverse the debit.

### Reasoning

Overdraft in this assignment is a value-day closing condition with a fee, not a gate on the debit command. Rejecting E7 would make E9 (`NOT_REVERSIBLE`) and would hide the Day 2 / Day 4 / Day 5 fee path the scenario is built around.

---

## 5. Overdraft uses ledger closing, not available (holds excluded from the test)

### Ambiguity

Available already subtracts holds. It is unclear whether a day is overdrawn when **booked ledger closing** is negative, or when **available** (closing minus outstanding holds) is negative.

### Decision

Overdraft is assessed only when **postings-only closing < 0**. Holds are not subtracted for that test. A day can have a large Auth-A hold and still accrue interest if the booked close is positive (Days 2–3). A fee is booked only for a negative ledger close, once per day (`netFee == 0`).

### Reasoning

Holds are reservations, not ledger postings. Charging an overdraft fee because of a hold would punish money that is still on the book. Settlement (E5) is the posting that can actually drive the close negative.

---

## 6. Interest rounding mode

### Ambiguity

Daily interest is described as 0.04% per day. The specification does not say how to round a fractional minor unit (for example AED 250.00 × 4 / 10000 = 0.10 exactly, but AED 465.00 × 4 / 10000 = 0.186).

### Decision

Rounding mode is **DOWN** (truncate toward zero for positive accruals): `(closing × 4) / 10000` in integer minor units. Configured as `ledger.rounding-mode: DOWN`. Zero or negative closing accrues `0`. Capitalization is the **sum of those daily accruals**, not a separately rounded total.

Examples: `25000 → 10`, `65000 → 26`, `46500 → 18`.

### Reasoning

Integer division with DOWN is deterministic, matches “no remainder left over after capitalization” when cap = sum of daily floors, and avoids inventing HALF_UP behaviour the spec never stated.

---

## 7. E10 instalment dates

### Ambiguity

E10 is a BHD 10.000 credit as three equal instalments with a single value date (Day 5) and no instalment schedule.

### Decision

All three rows (`E10:1`, `E10:2`, `E10:3`) use `valueDay = 5`. Amounts are `3334 / 3333 / 3333` minor units so the total stays BHD 10.000.

### Reasoning

Nothing in the spec justifies spreading instalments onto later days. Using Day 5 for all three keeps the supplied effective date. Identical 3.334 thirds would sum to 10.002; that extra 0.002 is recorded in [REJECTED.md](REJECTED.md).

---

## 8. BHD overdraft fee amount

### Ambiguity

The AED overdraft fee is AED 25.00. No BHD fee is given, and ACC-002 can in principle overdraw.

### Decision

BHD fee is **25.000** (25_000 minor units), the same 25 major-unit figure as AED, stored in `application.yaml`.

### Reasoning

The assignment needs a configured integer per currency. Using the same major-unit size as AED is explicit and invented; it is not derived from a hidden formula in the spec.
