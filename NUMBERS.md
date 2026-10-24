# NUMBERS.md

Every constant: the value, where it is used, why this value, and why not half of it.

---

## 1 · Currency scale

| Currency | Decimal places | One major unit | Used in |
|---|---|---|---|
| AED | 2 | 1 dirham = 100 fils (1200.00 → `120000`) | `ledger/enums/Currency.java` |
| BHD | 3 | 1 dinar = 1000 fils (10.000 → `10000`) | `ledger/enums/Currency.java` |


---

## 2 · Algorithmic constants


| Value | Meaning | Cost of changing it | Used in |
|---|---|---|---|
| `10` | decimal base, 10^scale | any other base stops being decimal money | `ledger/money/Money.java` |
| `2` | "half" is `2 × remainder ≥ denominator` | `1` would make it quarter-up | `ledger/money/Rounding.java` |
| `+1 / −1` | rounding direction: away from zero | flipping it makes rounding asymmetric | `ledger/money/Rounding.java` |
| `0`, `1` | zero, single step, "not found" | none; no meaning to assign | `ledger/money/` |

---
