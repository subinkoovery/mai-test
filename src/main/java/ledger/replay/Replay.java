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
import ledger.enums.Currency;
import ledger.journal.Journal;
import ledger.interest.Interest;
import ledger.journal.Ledger;
import ledger.money.Money;

/** Replays the stream and prints one line per account per day. */
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

        List<String> errors = new ArrayList<>();
        Map<String, Money> closed = new LinkedHashMap<>();
        Map<String, Money> holdsAtClose = new LinkedHashMap<>();

        for (Event event : events) {
            int day = openDay.get(event.accountId());
            while (day < event.processingDay()) {
                close.close(event.accountId(), day);
                interest.accrue(event.accountId(), day);
                closed.put(key(event.accountId(), day), ledger.balanceTo(event.accountId(), day));
                holdsAtClose.put(key(event.accountId(), day), authorisations.heldFor(event.accountId()));
                day++;
            }
            openDay.put(event.accountId(), event.processingDay());

            EventOutcome outcome = engine.process(event);
            if (outcome instanceof EventOutcome.Rejected rejected) {
                errors.add(event.eventId() + " " + rejected.code() + " - " + rejected.message());
            }
        }
        for (Map.Entry<String, Integer> entry : openDay.entrySet()) {
            for (int day = entry.getValue(); day <= LAST_DAY; day++) {
                close.close(entry.getKey(), day);
                interest.accrue(entry.getKey(), day);
                closed.put(key(entry.getKey(), day), ledger.balanceTo(entry.getKey(), day));
                holdsAtClose.put(key(entry.getKey(), day), authorisations.heldFor(entry.getKey()));
            }
        }
        for (String account : openDay.keySet()) {
            interest.capitalise(account, LAST_DAY, journal);
        }

        System.out.println("day | account | close    | fee    | accrual | holds");
        for (int day = 1; day <= LAST_DAY; day++) {
            for (String account : openDay.keySet()) {
                Money balance = closed.get(key(account, day));
                Money holds = holdsAtClose.get(key(account, day));
                System.out.printf("%3d | %-7s | %8s | %6s | %7s | %s%n",
                        day, account,
                        balance == null ? "-" : balance.format(),
                        feeOn(journal, account, day),
                        Money.ofMinor(interest.allocated(account, day),
                                ledger.currencyOf(account)).format(),
                        holds == null ? "-" : holds.format());
            }
        }
        System.out.println();
        for (String account : openDay.keySet()) {
            System.out.println("final " + account + ": " + ledger.balanceTo(account, LAST_DAY).format());
        }
        System.out.println();
        System.out.println("authorisations: Auth-A " + authorisations.stateOf("Auth-A")
                + ", Auth-B " + authorisations.stateOf("Auth-B"));
        if (errors.isEmpty()) {
            System.out.println("errors: none");
        } else {
            errors.forEach(error -> System.out.println("error: " + error));
        }
    }

    private static String key(String account, int day) {
        return account + "#" + day;
    }

    private static String feeOn(Journal journal, String account, int day) {
        return journal.snapshot().stream()
                .filter(p -> p.accountId().equals(account)
                        && p.valueDay() == day
                        && p.kind() == ledger.enums.PostingKind.FEE)
                .findFirst()
                .map(p -> p.amount().format())
                .orElse("-");
    }
}
