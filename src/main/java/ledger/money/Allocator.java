package ledger.money;

import java.util.Objects;

/** Splits a total into parts that always add back to it. */
public final class Allocator {

    private Allocator() {
    }

    /**
     * Rounds each numerator / denominator, then makes the parts sum to target.
     * The residual lands on the last non-zero part.
     */
    public static long[] reconcile(long target, long[] numerators, long denominator) {
        Objects.requireNonNull(numerators, "numerators required");
        if (numerators.length == 0) {
            throw new IllegalArgumentException("at least one part required");
        }
        if (denominator <= 0) {
            throw new IllegalArgumentException("denominator must be positive: " + denominator);
        }

        long[] parts = new long[numerators.length];
        long sum = 0;
        for (int i = 0; i < numerators.length; i++) {
            parts[i] = Rounding.halfUp(numerators[i], denominator);
            sum = Math.addExact(sum, parts[i]);
        }

        long residual = Math.subtractExact(target, sum);
        if (residual != 0) {
            int last = lastNonZero(numerators);
            if (last < 0) {
                throw new IllegalStateException("no accruing part to absorb residual " + residual);
            }
            parts[last] = Math.addExact(parts[last], residual);
            if (parts[last] < 0) {
                throw new IllegalStateException("residual " + residual + " makes part negative");
            }
        }
        return parts;
    }

    /** Equal instalments; the last one absorbs the residual. */
    public static long[] splitEven(long total, int parts) {
        if (parts <= 0) {
            throw new IllegalArgumentException("at least one part required: " + parts);
        }
        long base = total / parts;
        long[] out = new long[parts];
        for (int i = 0; i < parts; i++) {
            out[i] = base;
        }
        out[parts - 1] = Math.addExact(out[parts - 1], total - base * parts);
        return out;
    }

    private static int lastNonZero(long[] values) {
        for (int i = values.length - 1; i >= 0; i--) {
            if (values[i] != 0) {
                return i;
            }
        }
        return -1;
    }
}
