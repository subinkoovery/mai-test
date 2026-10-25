package ledger.replay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ledger.auth.AuthRegistry;
import ledger.close.DayClose;
import ledger.engine.Event;
import ledger.engine.EventOutcome;
import ledger.engine.EventProcessingEngine;
import ledger.enums.AuthState;
import ledger.enums.Currency;
import ledger.enums.PostingKind;
import ledger.interest.Interest;
import ledger.journal.Journal;
import ledger.journal.Ledger;
import ledger.journal.Posting;
import ledger.money.Money;

/** Replays the stream and prints, per day: closing balance, fee, auth states, errors. */
public final class Replay {

    private static final int LAST_DAY = 6;

    private Replay() {
    }

    public static void main(String[] args) throws IOException {
        List<Event> events = EventCsv.read(Path.of(args.length > 0 ? args[0] : "data/events.csv"));

        Journal journal = new Journal();
        Ledger ledger = new Ledger(journal,
                Map.of("ACC-001", Currency.AED, "ACC-002", Currency.BHD));
        AuthRegistry authorisations = new AuthRegistry(ledger);
        EventProcessingEngine engine = new EventProcessingEngine(journal, authorisations);
        DayClose close = new DayClose(journal, ledger);
        Interest interest = new Interest(ledger);

        Map<String, Integer> openDay = new LinkedHashMap<>();
        openDay.put("ACC-001", 1);
        openDay.put("ACC-002", 1);

        Map<String, Money> closed = new LinkedHashMap<>();
        Map<String, String> authAtClose = new LinkedHashMap<>();
        Map<String, List<String>> errorsByKey = new LinkedHashMap<>();

        for (Event event : events) {
            int day = openDay.get(event.accountId());
            while (day < event.processingDay()) {
                close.close(event.accountId(), day);
                closed.put(key(event.accountId(), day), ledger.balanceTo(event.accountId(), day));
                authAtClose.put(key(event.accountId(), day),
                        formatStates(authorisations.states(event.accountId())));
                day++;
            }
            openDay.put(event.accountId(), event.processingDay());

            EventOutcome outcome = engine.process(event);
            if (outcome instanceof EventOutcome.Rejected rejected) {
                errorsByKey.computeIfAbsent(key(event.accountId(), event.processingDay()),
                        k -> new ArrayList<>()).add(event.eventId() + " " + rejected.code());
            }
        }
        for (Map.Entry<String, Integer> entry : openDay.entrySet()) {
            for (int day = entry.getValue(); day <= LAST_DAY; day++) {
                close.close(entry.getKey(), day);
                closed.put(key(entry.getKey(), day), ledger.balanceTo(entry.getKey(), day));
                authAtClose.put(key(entry.getKey(), day),
                        formatStates(authorisations.states(entry.getKey())));
            }
        }
        for (String account : openDay.keySet()) {
            interest.capitalise(account, LAST_DAY, journal);
        }

        System.out.printf("%-3s | %-7s | %8s | %6s | %-28s | %s%n",
                "day", "account", "closing", "fee", "authorisations", "errors");
        for (int day = 1; day <= LAST_DAY; day++) {
            for (String account : openDay.keySet()) {
                Money balance = closed.get(key(account, day));
                List<String> errors = errorsByKey.getOrDefault(key(account, day), List.of());
                System.out.printf("%3d | %-7s | %8s | %6s | %-28s | %s%n",
                        day, account,
                        balance == null ? "-" : balance.format(),
                        feeOn(journal, account, day),
                        authAtClose.getOrDefault(key(account, day), "-"),
                        errors.isEmpty() ? "-" : String.join("; ", errors));
            }
        }
        System.out.println();
    }

    private static String key(String account, int day) {
        return account + "#" + day;
    }

    private static String formatStates(Map<String, AuthState> states) {
        if (states.isEmpty()) {
            return "-";
        }
        StringBuilder text = new StringBuilder();
        states.forEach((id, state) -> {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(id).append(' ').append(state);
        });
        return text.toString();
    }

    private static String feeOn(Journal journal, String account, int day) {
        return journal.snapshot().stream()
                .filter(p -> p.accountId().equals(account)
                        && p.valueDay() == day
                        && p.kind() == PostingKind.FEE)
                .findFirst()
                .map(p -> p.amount().format())
                .orElse("-");
    }

    /** A day's balance as it stood at the end of another day, before fees. */
    private static Money valueDayBalance(Ledger ledger, String account, int day, int asOf) {
        return ledger.balanceBeforeFeesAt(account, day, asOf);
    }
}
