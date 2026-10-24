package ledger.enums;

/**
 * What a posting represents, for display and reporting only.
 *
 * <p>The sign lives in the amount: a debit is negative, a credit is positive.
 * Nothing may derive a sign from this enum, or the label and the arithmetic
 * can disagree.
 */
public enum PostingKind {
    CREDIT,
    DEBIT,
    FEE,
    INTEREST,
    SETTLEMENT,
    REVERSAL
}
