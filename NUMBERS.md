# NUMBERS.md

Numeric constants and numeric design choices used by the ledger. Amounts are `long` minor units. Config lives in `application.yaml` unless a source file is named.

**Specification-mandated** means the assignment brief (as recorded in `ARCHITECTURE.md`, `AMBIGUITIES.md`, `REJECTED.md`, or the E1–E10 fixture) requires that value. **Implementation-selected** means the repo chose it because the brief was silent or internally inconsistent.

---

## 1. AED scale = 2

AED uses two decimal places. 1 AED = 100 minor units. AED 25.00 is stored as `2500`.

**Where.** `Currency.AED(2)`, `application.yaml` `ledger.currencies.AED.scale: 2`, `CurrencyTest`. Runtime formatting (`MinorUnits.format`) uses the enum. Yaml is loaded into `LedgerConfig` and asserted by `ShippedConfigTest`; it is not what `Currency.fromCode` reads.

**Why this value.** The brief requires two decimal places (fils). Scale 1 would drop valid fils. Scale 4 would invent precision the brief does not give.

**Why not half.** Scale 1 is one decimal place, not AED.

**Origin.** Specification-mandated.

---

## 2. BHD scale = 3

BHD uses three decimal places. 1 BHD = 1000 minor units. BHD 10.000 is stored as `10000`.

**Where.** `Currency.BHD(3)`, `application.yaml` `ledger.currencies.BHD.scale: 3`, `CurrencyTest`. Same enum/yaml split as AED.

**Why this value.** E10 is BHD 10.000. Scale 2 would drop the last fils. Scale 4 would invent unsupported precision.

**Why not half.** “Half of 3” is not a currency scale. Scale 1 or 2 would lose the third decimal the brief uses.

**Origin.** Specification-mandated.

---

## 3. AED overdraft fee = 2500 minor units (AED 25.00)

Posted as a negative `OVERDRAFT_FEE` of `−2500` when that day’s postings-only closing is negative and `netFee == 0`. Yaml stores the magnitude; `EndOfDayService` negates it.

**Where.** `application.yaml` `ledger.overdraft.fees-in-minor-units.AED: 2500`. `OverdraftPolicy.feeInMinorUnits(AED)`.

**Why this value.** The brief’s AED overdraft fee is AED 25.00. At scale 2 that is 2500 fils.

**Why not half.** `1250` is AED 12.50, not the stated fee.

**Origin.** Specification-mandated.

---

## 4. BHD overdraft fee = 25000 minor units (BHD 25.000)

Same 25 major-unit figure as AED, at BHD scale 3.

**Where.** `application.yaml` `ledger.overdraft.fees-in-minor-units.BHD: 25000`. `OverdraftPolicy` requires a positive fee for every `Currency`. `AMBIGUITIES.md` §8.

**Why this value.** ACC-002 can overdraw; the assignment needs an integer fee per currency. The brief gives AED 25.00 and no BHD tariff. The implementation copies the 25 major-unit size.

**Why not half.** `12500` (BHD 12.500) is equally invented and no longer parallel to the AED 25.00 figure that was copied.

**Origin.** Implementation-selected. Not in the brief.

---

## 5. Daily interest rate = 4 / 10000 (= 0.04% = 0.0004)

Accrual is integer

```text
floor(postingsOnlyClosing × 4 / 10000)
```

when closing > 0, else `0`. Examples in `InterestPolicyTest`: `25000 → 10`, `65000 → 26`, `46500 → 18`.

**Where.** `application.yaml`

```yaml
interest:
  daily-rate-numerator: 4
  daily-rate-denominator: 10000
```

`InterestPolicy.dailyAccrual`. `AMBIGUITIES.md` §6. `ARCHITECTURE.md` §4 / §12.

**Why this value.**

```text
0.04% = 0.04 / 100 = 0.0004 = 4 / 10000
```

The fraction keeps the rate exact in integer arithmetic. No `double`.

**Why not half.** `2 / 10000` is 0.02%, not 0.04%. `4 / 20000` is the same rate with a different encoding; the shipped pair is `4 / 10000`.

**Origin.** Rate is specification-mandated. The `4 / 10000` encoding is the implementation’s integer form of that rate.

---

## 6. Rounding mode = DOWN

`InterestPolicy` accepts only `RoundingMode.DOWN`. Other modes fail construction (`UNSUPPORTED_ROUNDING_MODE`). Integer division of `(closing × numerator) / denominator` is the DOWN truncation.

**Where.** `application.yaml` `ledger.rounding-mode: DOWN`. `InterestPolicy` constructor. `AMBIGUITIES.md` §6.

**Why this value.** The brief states 0.04% per day and that rounded daily accruals must sum to capitalization. It does not say HALF_UP. DOWN is deterministic. Capitalization is the sum of those daily floors, so there is no leftover remainder to discard (`REJECTED.md` §3).

**Why not half.** Rounding is not a magnitude. HALF_UP (or any other mode) is a different policy the brief never stated. Example: `46500 × 4 / 10000 = 18.6` → DOWN `18`, HALF_UP `19`.

**Origin.** Implementation-selected. Spec silent on fractional minor units.

---

## 7. Simulation window = 6 days

`SimulationEngine` loops `day = 1 .. windowDays`, dispatches that day’s arrivals, then `EOD-D{day}`.

**Where.** `application.yaml` `ledger.window-days: 6`. `SimulationEngine`, `ReportBuilder`, `ReplayMain`.

**Why this value.** The assignment is a six-day replay (E1–E10 plus daily close). Days are 1-indexed.

**Why not half.** A 3-day window never reaches E7–E10 (days 5–6) or day-6 capitalization.

**Origin.** Specification-mandated.

---

## 8. Capitalization day = 6

After the window, `SimulationEngine` dispatches `CapitalizeInterestCommand` per seed account with `valueDay = capitalizationDay`. The credit equals `sum(accruedDailyInterestByDay)`, not a separately rounded total.

**Where.** `application.yaml` `ledger.interest.capitalization-day: 6`. `SimulationEngine`, `EndOfDayService.capitalize`.

**Why this value.** Interest is capitalized at the end of the six-day window.

**Why not half.** Day 3 would capitalize before E7/E9/E10 and before days 4–6 have accrued.

**Origin.** Specification-mandated (end of the six-day window). Binding it to config key `capitalization-day: 6` (rather than hard-coding “after last EOD”) is implementation.

---

## 9. E10 instalment count = 3

`CreditInstalmentsCommand` for E10 uses `parts = 3`. `BookingService` appends `E10:1`, `E10:2`, `E10:3` in one commit, all on `valueDay = 5`.

**Where.** `ScenarioFixture` `new CreditInstalmentsCommand("E10", "ACC-002", 5, 10_000L, 3)`. `AMBIGUITIES.md` §7.

**Why this value.** The brief says three equal instalments of one BHD 10.000 credit.

**Why not half.** One or two parts is not three instalments.

**Origin.** Count and total are specification-mandated. Putting all three on day 5 is an implementation reading of “one value date, no schedule” (`AMBIGUITIES.md` §7).

---

## 10. E10 split = 3334 / 3333 / 3333 (total 10000)

```text
10000 / 3 = 3333 remainder 1
→ 3334, 3333, 3333
= BHD 3.334 + 3.333 + 3.333
= BHD 10.000
```

`MinorUnits.splitEvenly` assigns each leftover minor unit to the earliest parts (`base + 1` while `i < remainder`).

**Where.** `MinorUnits.splitEvenly`, `CreditInstalmentsTest`, `REJECTED.md` §2.

**Why this value.** Three identical 3.334 amounts sum to 10.002 and invent BHD 0.002. Conservation of the original 10.000 wins over identical rounded thirds.

**Why not half.** `1667 / 1666 / 1667` (or any 5000 total) is BHD 5.000, not 10.000. Equal `3333` thirds sum to 9999 and lose 1 fils.

**Origin.** Split rule is implementation-selected. The brief’s “each 3.334” is rejected in `REJECTED.md`.

---

## 11. Opening balances = 0

Both YAML seeds open at `0` minor units.

**Where.** `application.yaml` `accounts[].opening-balance-in-minor-units: 0`. `Account` constructor copies this into `amountInMinorUnits`.

**Why this value.** ACC-001 / ACC-002 start empty; day-1 close is E1−E2, not an inherited balance.

**Why not half.** Half of zero is still zero. A non-zero open would change every closing in `ARCHITECTURE.md` §12.

**Origin.** Specification scenario (zero start), encoded as config.

---

## 12. Hold start offset = lastClosedDay + 1

Approved and declined authorizations store `heldFromDay = lastClosedDay + 1`. Before any close, `lastClosedDay()` is `0` (`orElse(0)`), so the first hold is day 1.

**Where.** `AuthorizationService`, `Account.lastClosedDay()`.

**Why this value.** A hold should start on the next calendar day after the last closed day (or day 1 if nothing is closed). Reports count approved holds with `heldFromDay <= day` and no settlement `valueDay <= day`.

**Why not half.** `+ 0` would back-date the hold onto an already closed day. There is no “half day” in this integer calendar.

**Origin.** Implementation-selected. Brief defines holds vs ledger, not this offset.

---

## 13. Assignment scenario amounts (E1–E10)

As currently encoded in `ScenarioFixture.assignment()`. These are replay inputs, not tariff policy. Arrival day is the simulation day the command is dispatched; `valueDay` on the command may differ (E7).

| Id | Arrival | Command | Minor units | Money |
|---|---|---|---|---|
| E1 | 1 | credit ACC-001, value day 1 | 120000 | AED 1,200.00 |
| E2 | 1 | debit ACC-001, value day 1 | 95000 | AED 950.00 |
| E3 | 2 | authorize Auth-A | 20000 | AED 200.00 |
| E4 | 3 | credit ACC-001, value day 3 | 40000 | AED 400.00 |
| E5 | 4 | settle Auth-A, value day 4 | 18500 | AED 185.00 |
| E6 | 4 | settle Auth-Z, value day 4 | 18000 | AED 180.00 |
| E7 | 5 | debit ACC-001, value day 2 | 62000 | AED 620.00 |
| E8 | 5 | authorize Auth-B | 9000 (`90_00L`) | AED 90.00 |
| E9 | 6 | reverse E7 | — | — |
| E10 | 5 | credit ACC-002, value day 5, 3 parts | 10000 | BHD 10.000 |

**Why not half.** Half of any of these is a different supplied event (e.g. E1 at 60000 is AED 600.00, not 1,200.00). Goldens in `ARCHITECTURE.md` §12 would not hold.

**Origin.** Specification scenario, as encoded after the 2026-08-29 fixture corrections (see `WORKLOG.md`). E8 in source is `90_00L` = 9000, not `90_000L`. `ARCHITECTURE.md` §12 still says “authorize 90000”; that figure is not what `ScenarioFixture` dispatches. The brief PDF is not in this repo, so E6/E8 cannot be re-checked against the original text here.

---

## 14. Other numeric behaviour (not configured magnitudes)

- Overdraft test is `postingsOnlyClosing < 0` with `netFee == 0`. Zero is the “no fee yet” marker, not a fee amount.
- Interest is `0` when postings-only closing ≤ 0.
- Capitalization is skipped when summed accruals `== 0`.
- `ReconcileFromDayCommand` starts at `closingBalance(fromDay - 1)`.
- Optimistic version starts at `0` and increments per successful mutation. Not a business rate.

No other ledger magnitudes appear in `application.yaml`.
