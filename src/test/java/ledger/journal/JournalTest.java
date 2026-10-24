package ledger.journal;

import static ledger.TestRunner.assertEquals;
import static ledger.TestRunner.assertThrows;
import static ledger.TestRunner.assertTrue;

import java.util.List;

import ledger.TestRunner;
import ledger.enums.Currency;
import ledger.enums.PostingKind;
import ledger.money.Money;

/** Journal: append-only, and reads are copies. */
public final class JournalTest {

    private JournalTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("append grows the log by exactly one and assigns increasing ids", () -> {
                    Journal journal = new Journal();
                    assertEquals("empty", 0, journal.size());

                    Posting first = journal.append("ACC-001", PostingKind.CREDIT,
                            Money.parse("1200.00", Currency.AED), 1, 1, "E1", null);
                    Posting second = journal.append("ACC-001", PostingKind.DEBIT,
                            Money.parse("950.00", Currency.AED), 1, 1, "E2", null);

                    assertEquals("size", 2, journal.size());
                    assertEquals("first id", 1L, first.id());
                    assertEquals("second id", 2L, second.id());
                })

                .test("snapshot is a copy, not a view", () -> {
                    Journal journal = new Journal();
                    journal.append("ACC-001", PostingKind.CREDIT,
                            Money.parse("1200.00", Currency.AED), 1, 1, "E1", null);

                    List<Posting> before = journal.snapshot();
                    journal.append("ACC-001", PostingKind.DEBIT,
                            Money.parse("950.00", Currency.AED), 1, 1, "E2", null);

                    assertEquals("earlier snapshot is unchanged", 1, before.size());
                    assertEquals("journal has both", 2, journal.size());
                    assertThrows("snapshots are read-only", UnsupportedOperationException.class,
                            () -> before.add(before.get(0)));
                })

                .test("a reversal leaves the original posting untouched", () -> {
                    Journal journal = new Journal();
                    Posting debit = journal.append("ACC-001", PostingKind.DEBIT,
                            Money.parse("-620.00", Currency.AED), 2, 5, "E7", null);
                    journal.append("ACC-001", PostingKind.REVERSAL,
                            Money.parse("620.00", Currency.AED), 2, 6, "E9", debit.id());

                    List<Posting> all = journal.snapshot();
                    assertEquals("both entries kept", 2, all.size());
                    assertEquals("debit unchanged", "-620.00", all.get(0).amount().format());
                    assertTrue("debit still a debit", !all.get(0).isReversal());
                    assertEquals("reversal names the debit", debit.id(), all.get(1).referenceId());
                });
    }
}
