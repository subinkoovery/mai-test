package ledger.money;

import static ledger.TestRunner.assertEquals;
import static ledger.TestRunner.assertThrows;

import java.util.Arrays;

import ledger.TestRunner;

/** Allocation: the parts always sum to the total. */
public final class AllocatorTest {

    /** Six exact daily accruals, in tenths of a fils. */
    private static final long[] AED_ACCRUALS = {100, 100, 260, 186, 0, 176};

    private AllocatorTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("rounded accruals sum to the capitalised credit", () -> {
                    long[] parts = Allocator.reconcile(82, AED_ACCRUALS, 10);
                    assertEquals("parts", "[10, 10, 26, 19, 0, 17]", Arrays.toString(parts));
                    assertEquals("sum", 82L, sum(parts));
                    assertEquals("credit", "0.82", Money.ofMinor(sum(parts), Currency.AED).format());
                })

                .test("BHD instalments sum to the deposit", () -> {
                    long[] parts = Allocator.splitEven(10_000, 3);
                    assertEquals("parts", "[3333, 3333, 3334]", Arrays.toString(parts));
                    assertEquals("sum", 10_000L, sum(parts));
                })

                .test("an unreachable target is refused, not fudged", () -> {
                    assertThrows("no accruing day", IllegalStateException.class,
                            () -> Allocator.reconcile(1, new long[]{0, 0}, 10));
                    assertThrows("would go negative", IllegalStateException.class,
                            () -> Allocator.reconcile(-1, new long[]{10}, 10));
                });
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long value : values) {
            total += value;
        }
        return total;
    }
}
