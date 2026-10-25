package ledger.auth;

import java.util.Objects;

import ledger.enums.AuthState;
import ledger.money.Money;

/** One authorisation and the money it still reserves. */
public record Auth(
        String id,
        String accountId,
        Money requested,
        Money remainingHold,
        AuthState state) {

    public Auth {
        Objects.requireNonNull(id, "id required");
        Objects.requireNonNull(accountId, "accountId required");
        Objects.requireNonNull(requested, "requested required");
        Objects.requireNonNull(remainingHold, "remainingHold required");
        Objects.requireNonNull(state, "state required");
    }
}
