# REJECTED.md

## 1. Money as `BigDecimal` — rejected

Initially chose `BigDecimal` + `Currency`. Fell back to `long` minor units.

1. `equals` compares scale: `1200.00` and `1200.000` are unequal objects.
2. Illegal amounts like `12.005 AED` become representable.
3. Rounding moves to every division (`setScale`) instead of one boundary.

Result: `Money(long minor, Currency ccy)`, the ISO 4217 minor-unit model after research.

## 2. Holds as postings — rejected

A hold would enter the journal as a negative posting.

1. Capture double-counts: hold -200, then capture -200, for one purchase.
2. An unsettled hold looks like a permanent debit.

Result: holds live in `AuthRegistry`. The ledger holds only money that moved.

## 3. A `PENDING` authorisation state — rejected

`PENDING` exists only when approval waits on an issuer.

1. Here the balance is in memory; the decision is immediate.
2. It would need a timeout and an expiry rule the brief never gives.
3. The state could never be observed, so it would be dead code.

Result: `AuthState` has `APPROVED`, `DECLINED`, `SETTLED` only.
