package ledger.money;

import static ledger.TestRunner.assertEquals;
import static ledger.TestRunner.assertThrows;

import ledger.TestRunner;

/** Money: exact, scale-aware, currency-safe. */
public final class MoneyTest {

    private MoneyTest() {
    }

    public static TestRunner suite() {
        return TestRunner.suite()

                .test("amounts parse and format at the currency's scale", () -> {
                    Money aed = Money.parse("1200.00", Currency.AED);
                    assertEquals("AED minor", 120_000L, aed.minor());
                    assertEquals("AED format", "1200.00", aed.format());

                    Money bhd = Money.parse("10.000", Currency.BHD);
                    assertEquals("BHD minor", 10_000L, bhd.minor());
                    assertEquals("BHD format", "10.000", bhd.format());

                    assertEquals("missing decimals are padded", "18.60",
                            Money.parse("18.6", Currency.AED).format());
                    assertEquals("negative", "-180.00",
                            Money.ofMinor(-18_000, Currency.AED).format());
                })

                .test("excess precision is rejected, never rounded", () -> {
                    assertThrows("0.186 AED", IllegalArgumentException.class,
                            () -> Money.parse("0.186", Currency.AED));
                    assertThrows("10.0025 BHD", IllegalArgumentException.class,
                            () -> Money.parse("10.0025", Currency.BHD));
                    assertThrows("letters", IllegalArgumentException.class,
                            () -> Money.parse("12a.00", Currency.AED));
                })

                .test("currency mismatch is rejected", () ->
                        assertThrows("AED plus BHD", IllegalArgumentException.class,
                                () -> Money.ofMinor(1, Currency.AED).plus(Money.ofMinor(1, Currency.BHD))))

                .test("signed postings sum to the restated Day 2 balance", () -> {
                    Money balance = Money.parse("1200.00", Currency.AED)
                            .minus(Money.parse("950.00", Currency.AED))
                            .minus(Money.parse("620.00", Currency.AED));
                    assertEquals("Day 2", "-370.00", balance.format());
                })

                .test("a reversal restores the balance it removed", () -> {
                    Money afterDebit = Money.parse("250.00", Currency.AED)
                            .minus(Money.parse("620.00", Currency.AED));
                    assertEquals("restored", "250.00",
                            afterDebit.plus(Money.parse("620.00", Currency.AED)).format());
                });
    }
}
