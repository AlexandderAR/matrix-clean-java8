package me.rerere.matrix.logging;

import me.rerere.matrix.commands.DeveloperCommand;

/** Global Matrix debug logger. */
public final class DebugLogger {
    public static final DebugLogger INSTANCE;
    private static final String PREFIX;

    static {
        PREFIX = "[Matrix|Debug] ".intern();
        INSTANCE = new DebugLogger();
    }

    private DebugLogger() { }

    private static String formatLog(String message) {
        return new StringBuilder(PREFIX).append(message).toString();
    }

    public static void printDebug(String message) {
        if (!DeveloperCommand.developerMode) return;
        String formatted = formatLog(message);
        System.out.println((Object) formatted);
    }
}
