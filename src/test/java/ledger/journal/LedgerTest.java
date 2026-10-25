package ledger.journal;

import static ledger.TestRunner.assertEquals;

import java.util.Map;

import ledger.TestRunner;
import ledger.enums.Currency;
import ledger.enums.PostingKind;
import ledger.money.Money;

/** Ledger: the balance follows valueDay, not the order postings were booked. */
public final class LedgerTest {

    private LedgerTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("a backdated posting changes an old day, and reversal restores it", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal, Map.of("ACC-001", Currency.AED));

                    journal.append("ACC-001", PostingKind.CREDIT,
                            Money.parse("1200.00", Currency.AED), 1, 1, "E1", null);
                    journal.append("ACC-001", PostingKind.DEBIT,
                            Money.parse("-950.00", Currency.AED), 1, 1, "E2", null);
                    journal.append("ACC-001", PostingKind.CREDIT,
                            Money.parse("400.00", Currency.AED), 3, 3, "E4", null);
                    journal.append("ACC-001", PostingKind.SETTLEMENT,
                            Money.parse("-185.00", Currency.AED), 4, 4, "E5", null);
                    journal.append("ACC-001", PostingKind.DEBIT,
                            Money.parse("-620.00", Currency.AED), 2, 5, "E7", null);

                    assertEquals("Day 1", "250.00", ledger.balanceTo("ACC-001", 1).format());
                    assertEquals("Day 2 restated (C1)", "-370.00", ledger.balanceTo("ACC-001", 2).format());
                    assertEquals("Day 5", "-155.00", ledger.balanceTo("ACC-001", 5).format());

                    journal.append("ACC-001", PostingKind.REVERSAL,
                            Money.parse("620.00", Currency.AED), 2, 6, "E9", 3L);

                    assertEquals("Day 2 restored", "250.00", ledger.balanceTo("ACC-001", 2).format());
                    assertEquals("all postings kept", 6, journal.size());
                });
    }
}
