package ledger.interest;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import ledger.enums.PostingKind;
import ledger.journal.Journal;
import ledger.journal.Ledger;
import ledger.money.Allocator;
import ledger.money.Money;
import ledger.money.Rounding;

/**
 * Daily accrual at 0.04%.
 */
public final class Interest {

    public static final long RATE_NUMERATOR = 4;
    public static final long RATE_DENOMINATOR = 10_000;

    private final Ledger ledger;
    private final Map<String, Map<Integer, Long>> exact = new HashMap<>();
    private final Map<String, long[]> allocated = new HashMap<>();

    public Interest(Ledger ledger) {
        this.ledger = ledger;
    }

    /** Records the day's exact accrual. Call after the close, so the fee is included. */
    public void accrue(String accountId, int day) {
        Money balance = ledger.balanceTo(accountId, day);
        long numerator = balance.isPositive()
                ? Math.multiplyExact(balance.minor(), RATE_NUMERATOR)
                : 0L;
        exact.computeIfAbsent(accountId, key -> new TreeMap<>()).put(day, numerator);
    }

    /** Rounds the exact total once, allocates it across the days, and posts one credit. */
    public Money capitalise(String accountId, int lastDay, Journal journal) {
        Map<Integer, Long> byDay = exact.getOrDefault(accountId, Map.of());
        long[] numerators = new long[lastDay];
        for (int day = 1; day <= lastDay; day++) {
            numerators[day - 1] = byDay.getOrDefault(day, 0L);
        }

        long total = 0;
        for (long numerator : numerators) {
            total = Math.addExact(total, numerator);
        }
        long creditMinor = Rounding.halfUp(total, RATE_DENOMINATOR);
        allocated.put(accountId, Allocator.reconcile(creditMinor, numerators, RATE_DENOMINATOR));

        Money credit = Money.ofMinor(creditMinor, ledger.currencyOf(accountId));
        if (creditMinor != 0) {
            journal.append(accountId, PostingKind.INTEREST, credit,
                    lastDay, lastDay, "DAY-" + lastDay + "-INTEREST", null);
        }
        return credit;
    }

    /** The allocated accrual for a day, in minor units. Zero before capitalisation. */
    public long allocated(String accountId, int day) {
        long[] parts = allocated.get(accountId);
        if (parts == null || day < 1 || day > parts.length) {
            return 0;
        }
        return parts[day - 1];
    }
}
