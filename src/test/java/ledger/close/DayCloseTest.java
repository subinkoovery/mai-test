package ledger.close;

import static ledger.TestRunner.assertEquals;
import static ledger.TestRunner.assertTrue;

import java.util.Map;

import ledger.TestRunner;
import ledger.auth.AuthRegistry;
import ledger.engine.Event;
import ledger.engine.EventProcessingEngine;
import ledger.enums.Currency;
import ledger.enums.PostingKind;
import ledger.journal.Journal;
import ledger.journal.Ledger;
import ledger.journal.Posting;
import ledger.money.Money;

/** DayClose: one 25.00 fee on Day 5, none on the restated Day 2, never twice. */
public final class DayCloseTest {

    private DayCloseTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("the fee is assessed once, on the day the balance turned negative", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal, Map.of("ACC-001", Currency.AED));
                    AuthRegistry auths = new AuthRegistry(ledger);
                    EventProcessingEngine engine = new EventProcessingEngine(journal, auths);
                    DayClose close = new DayClose(journal, ledger);

                    engine.process(new Event.Credit("E1", "ACC-001", Money.parse("1200.00", Currency.AED), 1, 1));
                    engine.process(new Event.Debit("E2", "ACC-001", Money.parse("950.00", Currency.AED), 1, 1));
                    close.close("ACC-001", 1);

                    engine.process(new Event.Authorize("E3", "ACC-001", "Auth-A", Money.parse("200.00", Currency.AED), 2, 2));
                    close.close("ACC-001", 2);

                    engine.process(new Event.Credit("E4", "ACC-001", Money.parse("400.00", Currency.AED), 3, 3));
                    close.close("ACC-001", 3);

                    engine.process(new Event.Settle("E5", "ACC-001", "Auth-A", Money.parse("185.00", Currency.AED), 4, 4));
                    engine.process(new Event.Settle("E6", "ACC-001", "Auth-Z", Money.parse("180.00", Currency.AED), 4, 4));
                    close.close("ACC-001", 4);

                    engine.process(new Event.Debit("E7", "ACC-001", Money.parse("620.00", Currency.AED), 2, 5));
                    engine.process(new Event.Authorize("E8", "ACC-001", "Auth-B", Money.parse("90.00", Currency.AED), 5, 5));
                    close.close("ACC-001", 5);

                    assertEquals("one fee", 1, countFees(journal));
                    Posting fee = feePosting(journal);
                    assertEquals("amount", "-25.00", fee.amount().format());
                    assertEquals("value day", 5, fee.valueDay());
                    assertEquals("Day 5 closes", "-180.00", ledger.balanceTo("ACC-001", 5).format());
                    assertEquals("Day 2 restates", "-370.00", ledger.balanceTo("ACC-001", 2).format());

                    assertTrue("re-closing Day 5 is a no-op", !close.close("ACC-001", 5));
                    assertEquals("still one fee", 1, countFees(journal));
                });
    }

    private static int countFees(Journal journal) {
        int count = 0;
        for (Posting posting : journal.snapshot()) {
            if (posting.kind() == PostingKind.FEE) {
                count++;
            }
        }
        return count;
    }

    private static Posting feePosting(Journal journal) {
        for (Posting posting : journal.snapshot()) {
            if (posting.kind() == PostingKind.FEE) {
                return posting;
            }
        }
        throw new AssertionError("no fee posting");
    }
}
