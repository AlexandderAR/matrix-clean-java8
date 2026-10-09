package me.rerere.matrix.commands.condition;

import java.util.List;
import java.util.Optional;
import me.rerere.matrix.util.NamedTimestamp;

/** A named profile with ordered timestamped actions. */
public final class ActionProfile {
    private final String name;
    private final List<NamedTimestamp> entries;
    private final int firstOption;
    private final int secondOption;

    public ActionProfile(String name, int firstOption,
            List<NamedTimestamp> entries, int secondOption) {
        this.name = name;
        this.firstOption = firstOption;
        this.entries = entries;
        this.secondOption = secondOption;
    }

    public List<NamedTimestamp> entries() { return entries; }
    public String name() { return name; }
    public int firstOption() { return firstOption; }
    public int secondOption() { return secondOption; }

    public void closeEntries() {
        entries.forEach(NamedTimestamp::resetTimestamp);
    }

    public Optional<NamedTimestamp> findNamedByTimestamp(String requestedName) {
        NamedTimestamp best = null;
        for (NamedTimestamp entry : entries) {
            String entryName = entry.getName();
            if (!entryName.equalsIgnoreCase(requestedName)) continue;
            if (best == null) {
                best = entry;
            } else {
                long entryTime = entry.getTimestamp();
                long bestTime = best.getTimestamp();
                if (entryTime < bestTime) best = entry;
            }
        }
        return Optional.ofNullable(best);
    }
}
