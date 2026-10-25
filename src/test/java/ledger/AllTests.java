package ledger;

import ledger.auth.AuthRegistryTest;
import ledger.close.DayCloseTest;
import ledger.engine.EngineTest;
import ledger.journal.JournalTest;
import ledger.journal.LedgerTest;
import ledger.money.AllocatorTest;
import ledger.money.MoneyTest;
import ledger.money.RoundingTest;

/** Entry point for the whole suite: {@code java -cp out ledger.AllTests}. */
public final class AllTests {

    private AllTests() {
    }

    public static void main(String[] args) {
        int exitCode = TestRunner.suite()
                .add(MoneyTest.suite())
                .add(RoundingTest.suite())
                .add(AllocatorTest.suite())
                .add(JournalTest.suite())
                .add(LedgerTest.suite())
                .add(AuthRegistryTest.suite())
                .add(EngineTest.suite())
                .add(DayCloseTest.suite())
                .run();
        System.exit(exitCode);
    }
}
