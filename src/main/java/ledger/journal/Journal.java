package ledger.journal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public int size() {
        return postings.size();
    }
}
