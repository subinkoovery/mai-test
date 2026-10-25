# WORKLOG.md

| Time             | Purpose |
|------------------|---|
| 2026-10-24 18:48 | Initial commit — empty repo with a README stub |
| 2026-10-24 19:33 | Initialise the Java project: package layout and a placeholder `Sample` class |
| 2026-10-25 00:13 | Money layer: `Currency` (AED 2, BHD 3), `Money` as `long` minor units, `Rounding.halfUp`, `Allocator` with its two residual rules |
| 2026-10-25 00:21 | Test suite: `TestRunner`, `AllTests`, and 9 cases for scale, precision, currency safety, reversal, rounding and allocation |
| 2026-10-25 01:35 | Journal, Posting and Ledger: append-only log, one entry, derived balance |
| 2026-10-25 09:59 | AuthRegistry with holds for authorizations of hold amounts |
