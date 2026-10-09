package me.rerere.matrix.commands.setup;

import java.util.List;

/** Result of the first response setup pass. */
public final class CommandSetupResult {
    public final List<String> fields;
    public final boolean trialPresent;
    public final Object matrix;

    CommandSetupResult(List<String> fields, boolean trialPresent, Object matrix) {
        this.fields = fields;
        this.trialPresent = trialPresent;
        this.matrix = matrix;
    }
}
