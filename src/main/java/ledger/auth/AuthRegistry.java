package ledger.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import ledger.enums.AuthState;
import ledger.journal.Ledger;
import ledger.money.Money;

/**
 * Authorisations and the holds they reserve. Holds never enter the journal,
 * so they change available balance only.
 */
public final class AuthRegistry {

    private final Ledger ledger;
    private final Map<String, Auth> authorisationsById = new LinkedHashMap<>();

    public AuthRegistry(Ledger ledger) {
        this.ledger = Objects.requireNonNull(ledger, "ledger required");
    }

    /**
     * Approves only if available stays non-negative after the hold:
     * balance - existing holds - amount >= 0.
     */
    public boolean authorize(String id, String accountId, Money amount, int day) {
        if (authorisationsById.containsKey(id)) {
            throw new IllegalArgumentException("duplicate authorisation: " + id);
        }
        Money available = ledger.balanceTo(accountId, day).minus(heldFor(accountId));
        boolean approved = available.minus(amount).minor() >= 0;

        authorisationsById.put(id, new Auth(id, accountId, amount,
                approved ? amount : Money.zero(amount.ccy()),
                approved ? AuthState.APPROVED : AuthState.DECLINED));
        return approved;
    }

    /** Captures against the hold and releases the remainder. False if unknown. */
    public boolean settle(String id, Money captured) {
        Auth auth = authorisationsById.get(id);
        if (auth == null) {
            return false;
        }
        if (auth.state() != AuthState.APPROVED) {
            throw new IllegalStateException("not approved: " + id + " is " + auth.state());
        }
        if (captured.minor() > auth.remainingHold().minor()) {
            throw new IllegalArgumentException(
                    "capture " + captured.format() + " exceeds hold " + auth.remainingHold().format());
        }
        authorisationsById.put(id, new Auth(id, auth.accountId(), auth.requested(),
                Money.zero(captured.ccy()), AuthState.SETTLED));
        return true;
    }

    public Money heldFor(String accountId) {
        Money total = Money.zero(ledger.currencyOf(accountId));
        for (Auth auth : authorisationsById.values()) {
            if (auth.accountId().equals(accountId) && auth.state() == AuthState.APPROVED) {
                total = total.plus(auth.remainingHold());
            }
        }
        return total;
    }

    public AuthState stateOf(String id) {
        Auth auth = authorisationsById.get(id);
        return auth == null ? null : auth.state();
    }

    public boolean isKnown(String id) {
        return authorisationsById.containsKey(id);
    }
}