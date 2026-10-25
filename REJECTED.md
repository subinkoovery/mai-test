# REJECTED.md

## Criteria verdicts

| Criteria | Verdict        | Reason |
|---|----------------|---|
| C1 — The Day 2 closing ledger balance, evaluated at end of Day 5 and before any fee is assessed, is AED −370.00. | **Accepted**   | By value date at end of Day 5: 1200.00 − 950.00 − 620.00 = −370.00, before any fee |
| C2 — E7 causes exactly one overdraft fee to be assessed, on Day 2. | **Refused**    | E7 is booked on Day 5, so the fee is assessed at the Day 5 close (−155.00 → −180.00). Day 2 closed at 250.00, before E7 existed |
| C3 — The Day 4 settlement of Auth-A must be accepted. | **Accepted**   | 185.00 ≤ the 200.00 hold; capture is against reserved funds and 15.00 is released. No fresh affordability check |
| C4 — Any settlement referencing an authorization ID not present in the ledger must be rejected and the funds must not leave the account. | **Accepted**   | Auth-Z has no record; E6 returns `Rejected(UNKNOWN_AUTH)` with no posting and no hold change |
| C5 — If Auth-B is approved, its hold reduces available balance but not ledger balance. | **Accepted**(in relaity Auth-B was declined) | Holds never enter the journal, so the rule is true — but Auth-B was declined (−155 − 90 < 0), so no such hold exists |
| C6 — The three BHD instalments in E10 must each be BHD 3.334. | **Refused**    | 3.334 × 3 = 10.002 ≠ 10.000. The only split that sums correctly is 3.333 / 3.333 / 3.334 |
| C7 — After E9, all balances and fees return to their pre-E7 values. | **Refused**    | Append-only: the 25.00 fee stays. Day 6 ends at 440.00 (440.82 capitalised), not the pre-E7 465.00 |
| C8 — If the rounded daily interest accruals do not sum to the capitalized total, the remainder is discarded. | **Refused**    | Per-day rounding gives 83 fils; the rounded total is 82. The residual is allocated (Day 6: 18 → 17), because the brief requires the daily accruals to sum exactly |

## Approaches rejected

### 1. Money as `BigDecimal` — rejected

Initially chose `BigDecimal` + `Currency`. Fell back to `long` minor units.

1. `equals` compares scale: `1200.00` and `1200.000` are unequal objects.
2. Illegal amounts like `12.005 AED` become representable.
3. Rounding moves to every division (`setScale`) instead of one boundary.

Result: `Money(long minor, Currency ccy)`, the ISO 4217 minor-unit model after research.

### 2. Holds as postings — rejected

A hold would enter the journal as a negative posting.

1. Capture double-counts: hold −200, then capture −200, for one purchase.
2. An unsettled hold looks like a permanent debit.

Result: holds live in `AuthRegistry`. The ledger holds only money that moved.

### 3. A `PENDING` authorisation state — rejected

`PENDING` exists only when approval waits on an issuer.

1. Here the balance is in memory; the decision is immediate.
2. It would need a timeout and an expiry rule the brief never gives.
3. The state could never be observed, so it would be dead code.

Result: `AuthState` has `APPROVED`, `DECLINED`, `SETTLED` only.
