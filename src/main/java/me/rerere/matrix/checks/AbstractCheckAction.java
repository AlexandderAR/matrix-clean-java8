package me.rerere.matrix.checks;

/** Base action carrying its check and label. */
public abstract class AbstractCheckAction {
    private final String label;
    private final CheckRuntimeState check;

    protected AbstractCheckAction(String label, CheckRuntimeState check) {
        this.label = label;
        this.check = check;
    }

    public abstract void loadFromConfig();

    public CheckRuntimeState getCheck() {
        return check;
    }

    public String getLabel() {
        return label;
    }
}
