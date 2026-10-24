package ledger;

import ledger.journal.JournalTest;
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
                .run();
        System.exit(exitCode);
    }
}
