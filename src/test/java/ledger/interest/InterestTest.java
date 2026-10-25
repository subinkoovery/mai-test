package ledger.interest;

import static ledger.TestRunner.assertEquals;

import java.util.Arrays;
import java.util.Map;

import ledger.TestRunner;
import ledger.auth.AuthRegistry;
import ledger.close.DayClose;
import ledger.engine.Event;
import ledger.engine.EventProcessingEngine;
import ledger.enums.Currency;
import ledger.enums.PostingKind;
import ledger.journal.Journal;
import ledger.journal.Ledger;
import ledger.journal.Posting;
import ledger.money.Money;

/** Interest: exact accruals, one capitalised credit, no drift. */
public final class InterestTest {

    private InterestTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("accruals sum exactly to the capitalised credit", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal, Map.of("ACC-001", Currency.AED));
                    AuthRegistry auths = new AuthRegistry(ledger);
                    EventProcessingEngine engine = new EventProcessingEngine(journal, auths);
                    DayClose close = new DayClose(journal, ledger);
                    Interest interest = new Interest(ledger);

                    engine.process(new Event.Credit("E1", "ACC-001", Money.parse("1200.00", Currency.AED), 1, 1));
                    engine.process(new Event.Debit("E2", "ACC-001", Money.parse("950.00", Currency.AED), 1, 1));
                    closeAndAccrue(close, interest, "ACC-001", 1);

                    engine.process(new Event.Authorize("E3", "ACC-001", "Auth-A", Money.parse("200.00", Currency.AED), 2, 2));
                    closeAndAccrue(close, interest, "ACC-001", 2);

                    engine.process(new Event.Credit("E4", "ACC-001", Money.parse("400.00", Currency.AED), 3, 3));
                    closeAndAccrue(close, interest, "ACC-001", 3);

                    engine.process(new Event.Settle("E5", "ACC-001", "Auth-A", Money.parse("185.00", Currency.AED), 4, 4));
                    closeAndAccrue(close, interest, "ACC-001", 4);

                    engine.process(new Event.Debit("E7", "ACC-001", Money.parse("620.00", Currency.AED), 2, 5));
                    closeAndAccrue(close, interest, "ACC-001", 5);

                    engine.process(new Event.Reverse("E9", "ACC-001", "E7", 6));
                    closeAndAccrue(close, interest, "ACC-001", 6);

                    Money credit = interest.capitalise("ACC-001", 6, journal);

                    assertEquals("credit", "0.82", credit.format());
                    assertEquals("allocated", "[10, 10, 26, 19, 0, 17]",
                            Arrays.toString(new long[]{
                                    interest.allocated("ACC-001", 1),
                                    interest.allocated("ACC-001", 2),
                                    interest.allocated("ACC-001", 3),
                                    interest.allocated("ACC-001", 4),
                                    interest.allocated("ACC-001", 5),
                                    interest.allocated("ACC-001", 6)}));
                    assertEquals("final balance", "440.82",
                            ledger.balanceTo("ACC-001", 6).format());
                    assertEquals("one INTEREST posting", 1, countInterest(journal));
                });
    }

    private static void closeAndAccrue(DayClose close, Interest interest, String account, int day) {
        close.close(account, day);
        interest.accrue(account, day);
    }

    private static int countInterest(Journal journal) {
        int count = 0;
        for (Posting posting : journal.snapshot()) {
            if (posting.kind() == PostingKind.INTEREST) {
                count++;
            }
        }
        return count;
    }
}
