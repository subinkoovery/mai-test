package ledger.replay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ledger.engine.Event;
import ledger.enums.Currency;
import ledger.money.Allocator;
import ledger.money.Money;

/** Reads data/events.csv into events. A row may post in instalments. */
public final class EventCsv {

    private EventCsv() {
    }

    public static List<Event> read(Path path) throws IOException {
        List<Event> events = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            events.addAll(parse(line));
        }
        return events;
    }

    private static List<Event> parse(String line) {
        String[] f = line.split(",", -1);
        String id = f[0];
        String type = f[1];
        String account = f[2];
        String amountText = f[3];
        Currency currency = Currency.valueOf(f[4]);
        int valueDay = Integer.parseInt(f[5]);
        int processingDay = Integer.parseInt(f[6]);
        String authId = f[7];
        String reverses = f[8];
        int splits = Integer.parseInt(f[9]);

        List<Event> events = new ArrayList<>();
        switch (type) {
            case "CREDIT" -> {
                Money amount = Money.parse(amountText, currency);
                if (splits <= 1) {
                    events.add(new Event.Credit(id, account, amount, valueDay, processingDay));
                } else {
                    long[] parts = Allocator.splitEven(amount.minor(), splits);
                    for (int i = 0; i < parts.length; i++) {
                        events.add(new Event.Credit(id + "-" + (i + 1), account,
                                Money.ofMinor(parts[i], currency), valueDay, processingDay));
                    }
                }
            }
            case "DEBIT" -> events.add(new Event.Debit(id, account,
                    Money.parse(amountText, currency), valueDay, processingDay));
            case "AUTHORIZATION" -> events.add(new Event.Authorize(id, account, authId,
                    Money.parse(amountText, currency), valueDay, processingDay));
            case "SETTLEMENT" -> events.add(new Event.Settle(id, account, authId,
                    Money.parse(amountText, currency), valueDay, processingDay));
            case "REVERSAL" -> events.add(new Event.Reverse(id, account, reverses, processingDay));
            default -> throw new IllegalArgumentException("unknown event type: " + type);
        }
        return events;
    }
}
