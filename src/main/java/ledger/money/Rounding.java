package ledger.money;

public final class Rounding {

    private Rounding() {
    }

    /** Rounds numerator / denominator to a whole minor unit. Denominator must be positive. */
    public static long halfUp(long numerator, long denominator) {
        if (denominator <= 0) {
            throw new IllegalArgumentException("denominator must be positive: " + denominator);
        }
        long quotient = numerator / denominator;
        long remainder = numerator % denominator;
        if (remainder == 0) {
            return quotient;
        }
        if (Math.abs(remainder) * 2 >= denominator) {
            quotient += numerator > 0 ? 1 : -1;
        }
        return quotient;
    }
}
