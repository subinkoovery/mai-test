package ledger.engine;

import static ledger.TestRunner.assertEquals;
import static ledger.TestRunner.assertTrue;

import java.util.Map;

import ledger.TestRunner;
import ledger.auth.AuthRegistry;
import ledger.enums.AuthState;
import ledger.enums.Currency;
import ledger.journal.Journal;
import ledger.journal.Ledger;
import ledger.money.Money;

/** Engine: a settlement settles a known hold, or leaves no trace at all. */
public final class EngineTest {

    private EngineTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("a settlement settles a known hold or leaves no trace", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal, Map.of("ACC-001", Currency.AED));
                    AuthRegistry auths = new AuthRegistry(ledger);
                    EventProcessingEngine eventProcessingEngine = new EventProcessingEngine(journal, auths);

                    eventProcessingEngine.process(new Event.Credit("E1", "ACC-001",
                            Money.parse("1200.00", Currency.AED), 1, 1));
                    eventProcessingEngine.process(new Event.Debit("E2", "ACC-001",
                            Money.parse("950.00", Currency.AED), 1, 1));
                    eventProcessingEngine.process(new Event.Authorize("E3", "ACC-001", "Auth-A",
                            Money.parse("200.00", Currency.AED), 2, 2));

                    int before = journal.size();
                    Money heldBefore = auths.heldFor("ACC-001");

                    EventOutcome unknown = eventProcessingEngine.process(new Event.Settle("E6", "ACC-001", "Auth-Z",
                            Money.parse("180.00", Currency.AED), 4, 4));

                    assertEquals("rejected", EventProcessingEngine.UNKNOWN_AUTH, unknown.code());
                    assertEquals("no posting", before, journal.size());
                    assertEquals("no hold touched", heldBefore.format(), auths.heldFor("ACC-001").format());

                    EventOutcome applied = eventProcessingEngine.process(new Event.Settle("E5", "ACC-001", "Auth-A",
                            Money.parse("185.00", Currency.AED), 4, 4));

                    assertTrue("posted", applied.isPosted());
                    assertEquals("amount", "-185.00",
                            ((EventOutcome.Posted) applied).posting().amount().format());
                    assertEquals("hold released", "0.00", auths.heldFor("ACC-001").format());
                    assertEquals("state", AuthState.SETTLED, auths.stateOf("Auth-A"));
                });
    }
}
