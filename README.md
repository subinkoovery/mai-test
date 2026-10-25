# Ledger core — in-memory account ledger

Six-day ledger in Java 17

## Requirements

- JDK 17 or newer

## Build and run — Windows PowerShell

```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out ledger.replay.Replay
```

## Build and run — Mac / Linux

```bash
javac -d out $(find src -name "*.java")
java -cp out ledger.replay.Replay
```

## Run the report

```
day | account |  closing |    fee | authorisations               | errors
  1 | ACC-001 |   250.00 |      - | -                            | -
  2 | ACC-001 |   250.00 |      - | Auth-A APPROVED              | -
  3 | ACC-001 |   650.00 |      - | Auth-A APPROVED              | -
  4 | ACC-001 |   465.00 |      - | Auth-A SETTLED               | E6 UNKNOWN_AUTH
  5 | ACC-001 |  -180.00 | -25.00 | Auth-A SETTLED, Auth-B DECLINED | -
  6 | ACC-001 |   440.00 |      - | Auth-A SETTLED, Auth-B DECLINED | -
```

| Column | Meaning |
|---|---|
| `closing` | balance at that day's close, after any fee |
| `fee` | overdraft fee booked that day, or `-` |
| `authorisations` | state of each authorisation at that close |
| `errors` | rejected events; the replay continues |

After the table, the **arrival log** lists every event in the order it was applied, with its value day (`vd`), processing day (`pd`) and outcome.

## Run the failing test

```powershell
java -cp out ledger.close.KnownGapTest
```

```
running 1 tests
  FAIL  a backdated debit should reopen Day 2 and assess a fee
        a fee on Day 2, because Day 2 is negative by value date: expected <1> but was <0>

0 passed, 1 failed
```

The failure is deliberate. `src/test/java/ledger/close/KnownGapTest.java` is the required failing test against this design.

## Documents

| File | Contents |
|---|---|
| `NUMBERS.md` | every constant, where it is used, why that value |
| `AMBIGUITIES.md` | each ambiguity, its resolution, the rejected alternative |
| `REJECTED.md` | criteria accepted or refused with the numbers, plus abandoned approaches |
| `WORKLOG.md` | timestamped build log |
