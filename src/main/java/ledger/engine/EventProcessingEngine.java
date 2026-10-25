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
        throw new IllegalStateException("unhandled event: " + event);
    }

    private Posting post(String accountId, PostingKind kind, Money amount,
                         int valueDay, int processingDay, String eventId) {
        return journal.append(accountId, kind, amount, valueDay, processingDay, eventId, null);
    }
}