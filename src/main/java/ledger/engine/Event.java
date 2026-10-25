package ledger.engine;

import java.util.Objects;

import ledger.money.Money;

/**
 * A stream event, before the engine decides what it means.
 *
 */
public sealed interface Event
        permits Event.Credit, Event.Debit, Event.Authorize, Event.Settle, Event.Reverse {

    String eventId();

    String accountId();

    int processingDay();

    record Credit(String eventId, String accountId, Money amount,
                  int valueDay, int processingDay) implements Event {
        public Credit {
            requireCommon(eventId, accountId, amount, valueDay, processingDay);
        }
    }

    record Debit(String eventId, String accountId, Money amount,
                 int valueDay, int processingDay) implements Event {
        public Debit {
            requireCommon(eventId, accountId, amount, valueDay, processingDay);
        }
    }

    record Authorize(String eventId, String accountId, String authId, Money amount,
                     int valueDay, int processingDay) implements Event {
        public Authorize {
            requireCommon(eventId, accountId, amount, valueDay, processingDay);
            Objects.requireNonNull(authId, "authId required");
        }
    }

    record Settle(String eventId, String accountId, String authId, Money amount,
                  int valueDay, int processingDay) implements Event {
        public Settle {
            requireCommon(eventId, accountId, amount, valueDay, processingDay);
            Objects.requireNonNull(authId, "authId required");
        }
    }

    record Reverse(String eventId, String accountId, String reversesEventId,
                   int processingDay) implements Event {
        public Reverse {
            Objects.requireNonNull(eventId, "eventId required");
            Objects.requireNonNull(accountId, "accountId required");
            Objects.requireNonNull(reversesEventId, "reversesEventId required");
            if (processingDay < 1) {
                throw new IllegalArgumentException("day must be 1 or later");
            }
        }
    }

    private static void requireCommon(String eventId, String accountId, Money amount,
                                      int valueDay, int processingDay) {
        Objects.requireNonNull(eventId, "eventId required");
        Objects.requireNonNull(accountId, "accountId required");
        Objects.requireNonNull(amount, "amount required");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("event amount must be positive: " + amount.format());
        }
        if (valueDay < 1 || processingDay < 1) {
            throw new IllegalArgumentException("day must be 1 or later");
        }
    }
}
