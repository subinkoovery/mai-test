# NUMBERS.md

Every constant: the value, where it is used, and why this value.

---

## 1 · Currency scale

| Value | Meaning | Used in |
|---|---|---|
| `2` | AED decimal places, 1 dirham = 100 fils | `ledger/enums/Currency.java:4` |
| `3` | BHD decimal places, 1 dinar = 1000 fils | `ledger/enums/Currency.java:5` |

---

## 2 · Overdraft fee

| Value | Meaning | Used in |
|---|---|---|
| `2 500` | 25.00 AED in fils | `ledger/close/FeeSchedule.java:14` |
| `1` | one assessment per (account, day) | `ledger/close/DayClose.java:28` |
| `< 0` | the fee needs a negative closing balance | `ledger/close/DayClose.java:31` |

---

## 3 · Interest

| Value | Meaning | Used in |
|---|---|---|
| `4` | numerator of the daily rate, 0.04% | `ledger/interest/Interest.java:20` |
| `10 000` | denominator, the rate as an exact fraction | `ledger/interest/Interest.java:21` |
| Day `6` | capitalisation after the Day 6 close | `ledger/replay/Replay.java:73` |

---


## 4 · Arithmetic and window

| Value | Meaning | Used in |
|---|---|---|
| `10` | decimal base, 10^scale | `ledger/money/Money.java:144` |
| `2` | "half" is `2 × remainder ≥ denominator` | `ledger/money/Rounding.java:18` |
| `+1 / −1` | rounding direction, away from zero | `ledger/money/Rounding.java:19` |
| `6` | the window, Day 1–6 | `ledger/replay/Replay.java:24` |

---

## 6 · Source data

| Value | Meaning | Used in |
|---|---|---|
| `E1`–`E10` | the stream as given: amounts, days, auth ids | `data/events.csv` |
| `3` | E10's instalments, split by `Allocator.splitEven` | `data/events.csv` → `ledger/replay/EventCsv.java` |
