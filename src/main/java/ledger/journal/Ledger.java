package ledger.journal;

import java.util.Map;
import java.util.Objects;

import ledger.enums.Currency;
import ledger.enums.PostingKind;
import ledger.money.Money;

/**
 * Balances derived from the journal. There is no stored balance: every call
 * folds postings by value day, and optionally by the day they were booked.
 */
public final class Ledger {

    private final Journal journal;
    private final Map<String, Currency> currencyAccountMap;

    public Ledger(Journal journal, Map<String, Currency> currencies) {
        this.journal = Objects.requireNonNull(journal, "journal required");
        this.currencyAccountMap = Map.copyOf(currencies);
    }

    /** Every posting with valueDay {@code <= day}, as the journal stands now. */
    public Money balanceTo(String accountId, int day) {
        return fold(accountId, day, Integer.MAX_VALUE, true);
    }

    /**
     * The balance of one value day, as it stood at the end of another day:
     * postings with valueDay {@code <= valueDay} that were booked by {@code asOfDay}.
     */
    public Money balanceAt(String accountId, int valueDay, int asOfDay) {
        return fold(accountId, valueDay, asOfDay, true);
    }

    /** As {@link #balanceAt}, ignoring fee postings — the view C1 asks for. */
    public Money balanceBeforeFeesAt(String accountId, int valueDay, int asOfDay) {
        return fold(accountId, valueDay, asOfDay, false);
    }

    private Money fold(String accountId, int valueDay, int asOfDay, boolean includeFees) {
        Money total = Money.zero(currencyOf(accountId));
        for (Posting posting : journal.snapshot()) {
            if (!posting.accountId().equals(accountId)) {
                continue;
            }
            if (posting.valueDay() > valueDay || posting.processingDay() > asOfDay) {
                continue;
            }
            if (!includeFees && posting.kind() == PostingKind.FEE) {
                continue;
            }
            total = total.plus(posting.amount());
        }
        return total;
    }

    public Currency currencyOf(String accountId) {
        Currency ccy = currencyAccountMap.get(accountId);
        if (ccy == null) {
            throw new IllegalArgumentException("unknown account: " + accountId);
        }
        return ccy;
    }
}
