package ledger.close;

import java.util.Map;

import ledger.enums.Currency;
import ledger.money.Money;

/**
 * Overdraft fee per currency.
 */
public final class FeeSchedule {

    private static final Map<Currency, Money> OVERDRAFT = Map.of(
            Currency.AED, Money.ofMinor(2_500, Currency.AED));

    private FeeSchedule() {
    }

    public static Money overdraft(Currency currency) {
        Money fee = OVERDRAFT.get(currency);
        if (fee == null) {
            throw new IllegalStateException("no overdraft fee defined for " + currency);
        }
        return fee;
    }
}
