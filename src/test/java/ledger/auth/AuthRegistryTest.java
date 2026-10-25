package ledger.auth;

import static ledger.TestRunner.assertEquals;
import static ledger.TestRunner.assertTrue;

import java.util.Map;

import ledger.TestRunner;
import ledger.enums.AuthState;
import ledger.enums.Currency;
import ledger.enums.PostingKind;
import ledger.journal.Journal;
import ledger.journal.Ledger;
import ledger.money.Money;

/** AuthRegistry: holds reserve money without moving it, and settle releases the rest. */
public final class AuthRegistryTest {

    private AuthRegistryTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("a hold is approved only if available stays non-negative", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal, Map.of("ACC-001", Currency.AED));
                    AuthRegistry auths = new AuthRegistry(ledger);

                    journal.append("ACC-001", PostingKind.CREDIT,
                            Money.parse("1200.00", Currency.AED), 1, 1, "E1", null);
                    journal.append("ACC-001", PostingKind.DEBIT,
                            Money.parse("-950.00", Currency.AED), 1, 1, "E2", null);

                    assertTrue("Auth-A fits: 250 - 200 = 50",
                            auths.authorize("Auth-A", "ACC-001", Money.parse("200.00", Currency.AED), 2));
                    assertEquals("hold", "200.00", auths.heldFor("ACC-001").format());
                    assertEquals("the ledger did not move", "250.00",
                            ledger.balanceTo("ACC-001", 2).format());

                    journal.append("ACC-001", PostingKind.CREDIT,
                            Money.parse("400.00", Currency.AED), 3, 3, "E4", null);
                    journal.append("ACC-001", PostingKind.SETTLEMENT,
                            Money.parse("-185.00", Currency.AED), 4, 4, "E5", null);
                    journal.append("ACC-001", PostingKind.DEBIT,
                            Money.parse("-620.00", Currency.AED), 2, 5, "E7", null);

                    assertEquals("available at Day 5", "-355.00",
                            ledger.balanceTo("ACC-001", 5).minus(auths.heldFor("ACC-001")).format());
                    assertTrue("Auth-B is declined", !auths.authorize(
                            "Auth-B", "ACC-001", Money.parse("90.00", Currency.AED), 5));
                    assertEquals("declined", AuthState.DECLINED, auths.stateOf("Auth-B"));
                    assertEquals("no hold created", "200.00", auths.heldFor("ACC-001").format());
                })

                .test("settlement captures against the hold and releases the rest", () -> {
                    Journal journal = new Journal();
                    Ledger ledger = new Ledger(journal, Map.of("ACC-001", Currency.AED));
                    AuthRegistry auths = new AuthRegistry(ledger);

                    journal.append("ACC-001", PostingKind.CREDIT,
                            Money.parse("1200.00", Currency.AED), 1, 1, "E1", null);
                    auths.authorize("Auth-A", "ACC-001", Money.parse("200.00", Currency.AED), 1);

                    assertTrue("capture 185 of 200",
                            auths.settle("Auth-A", Money.parse("185.00", Currency.AED)));
                    assertEquals("settled", AuthState.SETTLED, auths.stateOf("Auth-A"));
                    assertEquals("hold released", "0.00", auths.heldFor("ACC-001").format());
                    assertTrue("unknown auth is refused",
                            !auths.settle("Auth-Z", Money.parse("180.00", Currency.AED)));
                    assertEquals("no record created for Auth-Z", false, auths.isKnown("Auth-Z"));
                });
    }
}
