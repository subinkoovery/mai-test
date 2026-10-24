package ledger.money;

import ledger.enums.Currency;

import java.util.Objects;

/** An exact amount: whole minor units plus the currency that gives them scale. */
public record Money(long minor, Currency ccy) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(ccy, "currency required");
    }

    public static Money zero(Currency ccy) {
        return new Money(0, ccy);
    }

    public static Money ofMinor(long minor, Currency ccy) {
        return new Money(minor, ccy);
    }

    /** "1200.00" -> 120000. More decimals than the currency allows is rejected. */
    public static Money parse(String text, Currency ccy) {
        Objects.requireNonNull(ccy, "currency required");
        if (text == null) {
            throw new IllegalArgumentException("amount required");
        }

        String s = text.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("amount required");
        }

        boolean negative = false;
        char sign = s.charAt(0);
        if (sign == '+' || sign == '-') {
            negative = sign == '-';
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            throw new IllegalArgumentException("not a decimal amount: " + text);
        }

        int dot = s.indexOf('.');
        if (dot >= 0 && s.indexOf('.', dot + 1) >= 0) {
            throw new IllegalArgumentException("not a decimal amount: " + text);
        }

        String whole = dot < 0 ? s : s.substring(0, dot);
        String frac = dot < 0 ? "" : s.substring(dot + 1);
        if (frac.length() > ccy.scale()) {
            throw new IllegalArgumentException(
                    "more than " + ccy.scale() + " decimal places for " + ccy + ": " + text);
        }

        StringBuilder fraction = new StringBuilder(frac);
        while (fraction.length() < ccy.scale()) {
            fraction.append('0');
        }

        String digits = (whole.isEmpty() ? "0" : whole) + fraction;
        for (int i = 0; i < digits.length(); i++) {
            if (!Character.isDigit(digits.charAt(i))) {
                throw new IllegalArgumentException("not a decimal amount: " + text);
            }
        }

        long magnitude;
        try {
            magnitude = Long.parseLong(digits);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount out of range: " + text);
        }
        return new Money(negative ? -magnitude : magnitude, ccy);
    }

    /** Always exactly ccy.scale() decimal places. */
    public String format() {
        long absolute = Math.absExact(minor);
        long factor = factor();
        long whole = absolute / factor;
        long fraction = absolute % factor;

        StringBuilder out = new StringBuilder();
        if (minor < 0) {
            out.append('-');
        }
        out.append(whole);
        if (ccy.scale() > 0) {
            String digits = Long.toString(fraction);
            out.append('.');
            for (int i = digits.length(); i < ccy.scale(); i++) {
                out.append('0');
            }
            out.append(digits);
        }
        return out.toString();
    }

    @Override
    public String toString() {
        return format() + " " + ccy;
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minor, other.minor), ccy);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minor, other.minor), ccy);
    }

    public Money negate() {
        return new Money(Math.negateExact(minor), ccy);
    }

    public Money abs() {
        return minor < 0 ? negate() : this;
    }

    public boolean isNegative() {
        return minor < 0;
    }

    public boolean isZero() {
        return minor == 0;
    }

    public boolean isPositive() {
        return minor > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minor, other.minor);
    }

    private long factor() {
        long factor = 1;
        for (int i = 0; i < ccy.scale(); i++) {
            factor *= 10;
        }
        return factor;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "amount required");
        if (ccy != other.ccy) {
            throw new IllegalArgumentException("currency mismatch: " + ccy + " vs " + other.ccy);
        }
    }
}
