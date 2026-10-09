package me.rerere.matrix.config;

import me.rerere.matrix.commands.UpdateCommand;

/** Routes boolean option reads and writes to the shared option implementation. */
public final class BooleanOptionRouter {
    public BooleanOptionRouter() { }

    public final boolean isEnabled() {
        return UpdateCommand.isShutdownAfterUpdate();
    }

    public void setEnabled(boolean enabled) {
        UpdateCommand.setShutdownAfterUpdate(enabled);
    }

}
