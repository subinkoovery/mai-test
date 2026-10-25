package ledger.close;

import java.util.HashSet;
import java.util.Set;

import ledger.enums.PostingKind;
import ledger.journal.Journal;
import ledger.journal.Ledger;
import ledger.money.Money;

/**
 * The end-of-day job: assess the overdraft fee once per (account, day).
 * A closed day is final, even if a backdated posting arrives later.
 */
public final class DayClose {

    private final Journal journal;
    private final Ledger ledger;
    private final Set<CloseKey> closed = new HashSet<>();

    public DayClose(Journal journal, Ledger ledger) {
        this.journal = journal;
        this.ledger = ledger;
    }

    /** Returns true when this call assessed a fee. A second close is a no-op. */
    public boolean close(String accountId, int day) {
        if (closed.contains(new CloseKey(accountId, day))) {
            return false;
        }
        boolean overdrawn = ledger.balanceTo(accountId, day).isNegative();
        if (overdrawn) {
            Money fee = FeeSchedule.overdraft(ledger.currencyOf(accountId));
            journal.append(accountId, PostingKind.FEE, fee.negate(),
                    day, day, "DAY-" + day + "-FEE", null);
        }
        closed.add(new CloseKey(accountId, day));
        return overdrawn;
    }

    private record CloseKey(String accountId, int day) {
    }
}
