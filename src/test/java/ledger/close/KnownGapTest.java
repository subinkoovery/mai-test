package ledger.close;

import static ledger.TestRunner.assertEquals;

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

/**
 * KNOWN GAP — this test is expected to FAIL. It is the one deliverable the
 * brief asks for: a failing test against the design.
 */
public final class KnownGapTest {

    private KnownGapTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()
                //Expected failure
                .test("a backdated debit should reopen Day 2 and assess a fee", () -> {
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
                    close.close("ACC-001", 4);

                    engine.process(new Event.Debit("E7", "ACC-001", Money.parse("620.00", Currency.AED), 2, 5));
                    close.close("ACC-001", 5);

                    assertEquals("Day 2 restated", "-370.00", ledger.balanceTo("ACC-001", 2).format());

                    // Fails by design: keyed to the value day, Day 2 would carry a fee;
                    // this design keys fees to the processing day and closes a day for good,
                    // so Day 2 has none. Reveals: "once per day" is incomplete until the
                    // day the fee is keyed to is named.
                    assertEquals("a fee on Day 2, because Day 2 is negative by value date",
                            1, feesOn(journal, 2));
                });
    }

    public static void main(String[] args) {
        System.exit(suite().run());
    }

    private static int feesOn(Journal journal, int day) {
        int count = 0;
        for (Posting posting : journal.snapshot()) {
            if (posting.kind() == PostingKind.FEE && posting.valueDay() == day) {
                count++;
            }
        }
        return count;
    }
}
