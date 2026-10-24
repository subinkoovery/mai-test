package ledger.money;

import static ledger.TestRunner.assertEquals;

import ledger.TestRunner;

/** Rounding: half away from zero, once. */
public final class RoundingTest {

    private RoundingTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("half rounds away from zero and exact values are unchanged", () -> {
                    assertEquals("18.6 up", 19L, Rounding.halfUp(186, 10));
                    assertEquals("17.4 down", 17L, Rounding.halfUp(174, 10));
                    assertEquals("-18.6 away from zero", -19L, Rounding.halfUp(-186, 10));
                    assertEquals("exact", 26L, Rounding.halfUp(260, 10));
                });
    }
}
