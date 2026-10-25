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

                    EventOutcome wrongAccount = eventProcessingEngine.process(new Event.Settle(
                            "E6b", "ACC-002", "Auth-A", Money.parse("185.00", Currency.AED), 4, 4));
                    assertEquals("wrong account", EventProcessingEngine.ACCOUNT_MISMATCH,
                            wrongAccount.code());
                    assertEquals("no posting", before, journal.size());
                    assertEquals("hold still held", heldBefore.format(), auths.heldFor("ACC-001").format());

                    EventOutcome applied = eventProcessingEngine.process(new Event.Settle("E5", "ACC-001", "Auth-A",
                            Money.parse("185.00", Currency.AED), 4, 4));

                    assertTrue("posted", applied.isPosted());
                    assertEquals("amount", "-185.00",
                            ((EventOutcome.Posted) applied).posting().amount().format());
                    assertEquals("hold released", "0.00", auths.heldFor("ACC-001").format());
                    assertEquals("state", AuthState.SETTLED, auths.stateOf("Auth-A"));
                })

                .test("a reversal restores the value day without touching the original", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal, Map.of("ACC-001", Currency.AED));
                    AuthRegistry auths = new AuthRegistry(ledger);
                    EventProcessingEngine engine = new EventProcessingEngine(journal, auths);

                    engine.process(new Event.Credit("E1", "ACC-001", Money.parse("1200.00", Currency.AED), 1, 1));
                    engine.process(new Event.Debit("E2", "ACC-001", Money.parse("950.00", Currency.AED), 1, 1));
                    engine.process(new Event.Authorize("E3", "ACC-001", "Auth-A", Money.parse("200.00", Currency.AED), 2, 2));
                    engine.process(new Event.Credit("E4", "ACC-001", Money.parse("400.00", Currency.AED), 3, 3));
                    engine.process(new Event.Settle("E5", "ACC-001", "Auth-A", Money.parse("185.00", Currency.AED), 4, 4));
                    engine.process(new Event.Debit("E7", "ACC-001", Money.parse("620.00", Currency.AED), 2, 5));

                    assertEquals("Day 2 before reversal", "-370.00", ledger.balanceTo("ACC-001", 2).format());
                    int before = journal.size();

                    EventOutcome outcome = engine.process(new Event.Reverse("E9", "ACC-001", "E7", 6));

                    assertTrue("posted", outcome.isPosted());
                    assertEquals("amount", "620.00",
                            ((EventOutcome.Posted) outcome).posting().amount().format());
                    assertEquals("one new posting", before + 1, journal.size());
                    assertEquals("Day 2 restored", "250.00", ledger.balanceTo("ACC-001", 2).format());
                    assertEquals("E7 untouched", "-620.00",
                            journal.findByEventId("E7").orElseThrow().amount().format());
                    assertEquals("referenceId points at E7",
                            journal.findByEventId("E7").orElseThrow().id(),
                            ((EventOutcome.Posted) outcome).posting().referenceId());
                })

                .test("a reversal is refused if the target is unknown, elsewhere, or already reversed", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal,
                            Map.of("ACC-001", Currency.AED, "ACC-002", Currency.AED));
                    AuthRegistry auths = new AuthRegistry(ledger);
                    EventProcessingEngine engine = new EventProcessingEngine(journal, auths);

                    engine.process(new Event.Debit("E7", "ACC-001", Money.parse("620.00", Currency.AED), 2, 5));
                    int before = journal.size();

                    assertEquals("unknown target", EventProcessingEngine.UNKNOWN_POSTING,
                            engine.process(new Event.Reverse("E9", "ACC-001", "E99", 6)).code());
                    assertEquals("wrong account", EventProcessingEngine.ACCOUNT_MISMATCH,
                            engine.process(new Event.Reverse("E9", "ACC-002", "E7", 6)).code());
                    assertEquals("nothing appended by either", before, journal.size());

                    assertTrue("first reversal posts",
                            engine.process(new Event.Reverse("E9", "ACC-001", "E7", 6)).isPosted());
                    assertEquals("second reversal refused", EventProcessingEngine.ALREADY_REVERSED,
                            engine.process(new Event.Reverse("E10", "ACC-001", "E7", 6)).code());
                    assertEquals("only one reversal exists", before + 1, journal.size());
                });
    }
}
