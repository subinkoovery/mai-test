package ledger.journal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ledger.enums.PostingKind;
import ledger.money.Money;

/**
 * Append-only log of postings. The only mutation is {@link #append}; there is
 * no remove, replace or sort. {@link #snapshot()} returns a copy, not a view.
 */
public final class Journal {

    private final List<Posting> postings = new ArrayList<>();
    private long nextId = 1;

    public Posting append(
            String accountId,
            PostingKind kind,
            Money amount,
            int valueDay,
            int processingDay,
            String sourceEventId,
            Long referenceId) {
        Objects.requireNonNull(amount, "amount required");
        Posting posting = new Posting(nextId++, accountId, kind, amount,
                valueDay, processingDay, sourceEventId, referenceId);
        postings.add(posting);
        return posting;
    }

    public List<Posting> snapshot() {
        return List.copyOf(postings);
    }

    /** The posting booked for an event id, if any. Used to resolve a reversal. */
    public Optional<Posting> findByEventId(String eventId) {
        for (Posting posting : postings) {
            if (posting.sourceEventId() != null && posting.sourceEventId().equals(eventId)) {
                return Optional.of(posting);
            }
        }
        return Optional.empty();
    }

    /** True when a REVERSAL posting names this posting as its target. */
    public boolean isReversed(long postingId) {
        for (Posting posting : postings) {
            if (posting.kind() == PostingKind.REVERSAL
                    && Objects.equals(posting.referenceId(), postingId)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return postings.size();
    }
}
