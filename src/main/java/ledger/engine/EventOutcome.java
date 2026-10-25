package ledger.engine;

import java.util.Objects;

import ledger.journal.Posting;

/**
 * What the engine did with an event.
 */
public sealed interface EventOutcome
        permits EventOutcome.Posted, EventOutcome.Accepted, EventOutcome.Rejected {

    /** A posting was appended to the journal. */
    record Posted(Posting posting) implements EventOutcome {
        public Posted {
            Objects.requireNonNull(posting, "posting required");
        }
    }

    /** The event was handled without moving money (an authorisation decision). */
    record Accepted(String detail) implements EventOutcome {
        public Accepted {
            Objects.requireNonNull(detail, "detail required");
        }
    }

    /** The event was refused and nothing changed. */
    record Rejected(String code, String message) implements EventOutcome {
        public Rejected {
            Objects.requireNonNull(code, "code required");
        }
    }

    default boolean isPosted() {
        return this instanceof Posted;
    }

    default boolean isRejected() {
        return this instanceof Rejected;
    }

    default String code() {
        return this instanceof Rejected rejected ? rejected.code() : null;
    }
}