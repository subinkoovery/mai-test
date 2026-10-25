package ledger.journal;

import java.util.Objects;

import ledger.enums.PostingKind;
import ledger.money.Money;

/**
 * One booked entry. Immutable: a correction is a new posting, never an edit.
 *
 * <p>{@code valueDay} decides balances, {@code processingDay} decides fees.
 * {@code amount} carries the sign; {@code kind} is a label only.
 */
public record Posting(
        long id,
        String accountId,
        PostingKind kind,
        Money amount,
        int valueDay,
        int processingDay,
        String sourceEventId,
        Long referenceId) {

    public Posting {
        Objects.requireNonNull(accountId, "accountId required");
        Objects.requireNonNull(kind, "kind required");
        Objects.requireNonNull(amount, "amount required");
        if (valueDay < 1 || processingDay < 1) {
            throw new IllegalArgumentException("day must be 1 or later");
        }
    }

    /** A reversal is a REVERSAL posting that names the posting it cancels. */
    public boolean isReversal() {
        return kind == PostingKind.REVERSAL && referenceId != null;
    }
}
