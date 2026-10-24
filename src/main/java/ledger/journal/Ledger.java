package ledger.journal;

import java.util.Map;
import java.util.Objects;

import ledger.enums.Currency;
import ledger.money.Money;

/**
 * Balances derived from the journal. There is no stored balance: every call
 * folds the postings whose {@code valueDay} is on or before the day asked for.
 */
public final class Ledger {

    private final Journal journal;
    private final Map<String, Currency> currencyAccountMap;

    public Ledger(Journal journal, Map<String, Currency> currencies) {
        this.journal = Objects.requireNonNull(journal, "journal required");
        this.currencyAccountMap = Map.copyOf(currencies);
    }

    public Money balanceTo(String accountId, int day) {
        Currency ccy = currencyAccountMap.get(accountId);
        if (ccy == null) {
            throw new IllegalArgumentException("unknown account: " + accountId);
        }
        Money total = Money.zero(ccy);
        for (Posting posting : journal.snapshot()) {
            if (posting.accountId().equals(accountId) && posting.valueDay() <= day) {
                total = total.plus(posting.amount());
            }
        }
        return total;
    }
}
