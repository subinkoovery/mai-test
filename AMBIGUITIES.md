# AMBIGUITIES.md

| # | Ambiguity | Resolution | More info |
|---|---|---|---|
| 1 | How is a day represented? | `int` 1..6, no calendar date | `LocalDate` needs an invented start date |
| 2 | Can two currencies be combined? | No — `Money` throws on mismatch | Implicit FX needs a rate the system lacks |
| 3 | Which date drives balances? | `valueDay` drives balances; `processingDay` only for fees | |
| 4 | More decimals than the currency allows | Rejected at parse (`0.186` AED) | Rounding on ingest hides a feed error |
| 5 | Fewer decimals | Padded (`18.6` → `18.60`) | |
| 6 | Is a hold money? | No — lives in `AuthRegistry`, not the journal | |
| 7 | Duplicate authorisation id | Does not allow. Throw error | |
| 8 | Can a posting be reversed twice? | No — a posting cancels once; detected by a `REVERSAL` posting whose `referenceId` points at it | Allowing it lets a replayed event double the money |
