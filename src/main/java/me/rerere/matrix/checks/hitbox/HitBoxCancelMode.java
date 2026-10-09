package me.rerere.matrix.checks.hitbox;

/** Modes accepted by the hitbox cancellation setting. */
public enum HitBoxCancelMode {
    NONE,
    SILENT,
    CANCEL;

    public static HitBoxCancelMode resolve(String requestedName) {
        return resolveName(requestedName);
    }

    private static HitBoxCancelMode resolveName(String requestedName) {
        HitBoxCancelMode[] modes = values();
        for (int index = 0; index < modes.length; index++) {
            HitBoxCancelMode mode = modes[index];
            if (mode.name().equalsIgnoreCase(requestedName)) {
                return mode;
            }
        }
        me.rerere.matrix.Matrix.logMessage(
                unknownCancelMessage(requestedName));
        me.rerere.matrix.Matrix.logMessage(
                "(HitBox) Use default cancel way: Silent");
        return SILENT;
    }

    private HitBoxCancelMode() {
    }

    private static String unknownCancelMessage(String requestedName) {
        return new StringBuilder("(HitBox) Unknown cancel way: ")
                .append(requestedName)
                .toString();
    }
}
