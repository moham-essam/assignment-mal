# REJECTED.md

This document records assignment criteria that I evaluated against the implementation, and which of those I refuse as written.

---

## Criterion evaluation

| Criterion | Correct? | Why |
| --- | --- | --- |
| Day 2 balance at end of Day 5, before fees = AED −370 | Yes | 1200 − 950 − 620 = −370 |
| E7 causes exactly one overdraft fee, on Day 2 | **No** | The Day 2 fee changes later closings; Day 4 and Day 5 can also go negative and require fees |
| Day 4 settlement of Auth-A accepted | Yes | Auth-A exists and was approved |
| Settlement with a missing auth ID rejected; no funds leave | Yes | E6 references nonexistent Auth-Z |
| If Auth-B were approved, the hold would reduce available but not the ledger | Yes | That is the authorization rule (E8 is declined for insufficient available) |
| After E9, all balances and fees return to their pre-E7 values | Potentially yes under this design | E9 reverses E7; compensating `FEE_REVERSAL` rows restore effective balances and fees |
| E10 three instalments each BHD 3.334 | **No** | 3.334 × 3 = 10.002, which invents BHD 0.002 |
| Discard interest remainder if accruals do not equal capitalization | **No** | Conflicts with “rounded daily accruals must sum exactly to the capitalized total” |

The **No** rows are rejected below. The Yes / potentially-yes rows are implemented.

---

## Rejected acceptance criteria

### 1. “E7 causes exactly one overdraft fee to be assessed, on Day 2.”

**Rejected as written.**

E7 is received on Day 5 with `valueDay = 2` and amount AED −620.00.

Before E7, Day 2 ledger close is AED 1,200.00 − AED 950.00 = AED 250.00. After E7, postings-only Day 2 is AED −370.00, so Day 2 qualifies for an overdraft fee of AED −25.00. That fee is itself value-dated Day 2, so the effective Day 2 close becomes AED −395.00.

E4 then contributes AED +400.00 on Day 3 (−395 + 400 = AED 5.00). E5 settles Auth-A for AED −185.00 on Day 4 (5 − 185 = AED −180.00). Day 4 is therefore also negative and also qualifies for a fee. That Day 4 fee pulls Day 5 (no new customer postings) negative as well.

The non-negotiable rule is: assess an overdraft fee **once per day per account** when that day’s postings-only closing is negative. The system therefore cannot guarantee that E7 results in exactly one fee on Day 2. Historical fee calculation must include the downstream effect of earlier value-dated fees on later closings.

---

### 2. “The three BHD instalments in E10 must each be BHD 3.334.”

**Rejected.**

E10 credits BHD 10.000. Three identical instalments of BHD 3.334 would total 10.002 and create BHD 0.002 that did not exist in the original transaction.

The implementation keeps the original amount: 10.000 = 10_000 minor units. `10000 / 3 = 3333 remainder 1`. The residual is assigned to the first instalment:

```text
3334, 3333, 3333
=
BHD 3.334, BHD 3.333, BHD 3.333
=
BHD 10.000
```

Preserving the original monetary total is a stronger invariant than forcing the rounded instalments to be identical.

---

### 3. “If the rounded daily interest accruals do not sum to the capitalized total, the remainder is discarded.”

**Rejected.**

This conflicts with the non-negotiable rule:

> The rounded daily accruals must sum exactly to the capitalized total.

Capitalization is defined as the **sum of the final rounded daily accruals**. It is not calculated independently and then reconciled. There is therefore no remainder to discard. Discarding a difference would violate the equality requirement.

---

## Criterion not rejected

### “After E9, all balances and fees return to their pre-E7 values.”

Not rejected under this design.

E9 reverses E7 on the same historical value day. `ReconcileFromDayCommand` then walks from that day through the last closed day. Original `OVERDRAFT_FEE` rows stay; if a fee is no longer justified, a compensating `FEE_REVERSAL` is appended.

```text
DEBIT          −620
OVERDRAFT_FEE   −25
REVERSAL        +620
FEE_REVERSAL    +25
```

The journal stays append-only. Effective closings and net fees can return to the pre-E7 position.

---

## Decision principle

Where requirements conflict, priority is:

```text
1. explicit non-negotiable rules
2. conservation of monetary value
3. append-only history
4. value-date correctness
5. deterministic replay of closings from the journal
6. explicit compensating rows rather than hidden edits
```

A criterion is rejected only where following it would violate one of these stronger invariants.
