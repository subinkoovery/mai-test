# AMBIGUITIES.md

| # | Ambiguity | Resolution                                               | More info                                 |
|---|---|----------------------------------------------------------|-------------------------------------------|
| 1 | How is a day represented? | `int` 1..6, no calendar date                             | `LocalDate` needs an invented start date  |
| 2 | Can two currencies be combined? | No — `Money` throws on mismatch                          | Implicit FX needs a rate the system lacks |
| 3 | Which date drives balances? | `valueDay` drive balances; `processingDay` only for fees |                                           |
| 4 | More decimals than the currency allows | Rejected at parse (`0.186` AED)                          | Rounding on ingest hides a feed error     |
| 5 | Fewer decimals | Padded (`18.6` → `18.60`)                                |                                           |
| 7 | Is a hold money? | No — lives in `AuthRegistry`, not the journal            |                                           |
| 8 | Duplicate authorisation id | Does not allow. Throw error                              |     |
