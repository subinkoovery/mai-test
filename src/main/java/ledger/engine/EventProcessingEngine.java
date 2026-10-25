package ledger.engine;

import java.util.Objects;

import ledger.auth.AuthRegistry;
import ledger.enums.PostingKind;
import ledger.journal.Journal;
import ledger.journal.Posting;
import ledger.money.Money;

/**
 * Routes an event to the journal or the authorisation registry.
 */
public final class EventProcessingEngine {

    public static final String UNKNOWN_AUTH = "UNKNOWN_AUTH";
    public static final String UNKNOWN_POSTING = "UNKNOWN_POSTING";
    public static final String ACCOUNT_MISMATCH = "ACCOUNT_MISMATCH";
    public static final String ALREADY_REVERSED = "ALREADY_REVERSED";

    private final Journal journal;
    private final AuthRegistry authorisations;

    public EventProcessingEngine(Journal journal, AuthRegistry authorisations) {
        this.journal = Objects.requireNonNull(journal, "journal required");
        this.authorisations = Objects.requireNonNull(authorisations, "authorisations required");
    }

    public EventOutcome process(Event event) {
        if (event instanceof Event.Credit credit) {
            return new EventOutcome.Posted(post(credit.accountId(), PostingKind.CREDIT,
                    credit.amount(), credit.valueDay(), credit.processingDay(), credit.eventId()));
        }
        if (event instanceof Event.Debit debit) {
            return new EventOutcome.Posted(post(debit.accountId(), PostingKind.DEBIT,
                    debit.amount().negate(), debit.valueDay(), debit.processingDay(), debit.eventId()));
        }
        if (event instanceof Event.Authorize authorization) {
            boolean approved = authorisations.authorize(authorization.authId(),
                    authorization.accountId(), authorization.amount(), authorization.processingDay());
            return new EventOutcome.Accepted(authorization.authId() + (approved ? " approved" : " declined"));
        }
        if (event instanceof Event.Settle settlement) {
            if (!authorisations.isKnown(settlement.authId())) {
                return new EventOutcome.Rejected(UNKNOWN_AUTH,
                        "no authorisation for " + settlement.authId());
            }
            authorisations.settle(settlement.authId(), settlement.amount());
            return new EventOutcome.Posted(post(settlement.accountId(), PostingKind.SETTLEMENT,
                    settlement.amount().negate(), settlement.valueDay(),
                    settlement.processingDay(), settlement.eventId()));
        }
        if (event instanceof Event.Reverse reversal) {
            Posting original = journal.findByEventId(reversal.reversesEventId()).orElse(null);
            if (original == null) {
                return new EventOutcome.Rejected(UNKNOWN_POSTING,
                        "no posting for the reversal event " + reversal.reversesEventId());
            }
            if (!original.accountId().equals(reversal.accountId())) {
                return new EventOutcome.Rejected(ACCOUNT_MISMATCH,
                        "posting " + original.id() + " belongs to " + original.accountId());
            }
            if (journal.isReversed(original.id())) {
                return new EventOutcome.Rejected(ALREADY_REVERSED,
                        "posting " + original.id() + " is already reversed");
            }
            return new EventOutcome.Posted(journal.append(
                    original.accountId(),
                    PostingKind.REVERSAL,
                    original.amount().negate(),
                    original.valueDay(),
                    reversal.processingDay(),
                    reversal.eventId(),
                    original.id()));
        }
        throw new IllegalStateException("unhandled event: " + event);
    }

    private Posting post(String accountId, PostingKind kind, Money amount,
                         int valueDay, int processingDay, String eventId) {
        return journal.append(accountId, kind, amount, valueDay, processingDay, eventId, null);
    }
}