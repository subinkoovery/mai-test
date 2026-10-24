# REJECTED.md

## 1. Money as `BigDecimal` — rejected

Initially chose `BigDecimal` + `Currency`. Fell back to `long` minor units.

1. `equals` compares scale: `1200.00` and `1200.000` are unequal objects.
2. Illegal amounts like `12.005 AED` become representable.
3. Rounding moves to every division (`setScale`) instead of one boundary.

Result: `Money(long minor, Currency ccy)`, the ISO 4217 minor-unit model after research.
