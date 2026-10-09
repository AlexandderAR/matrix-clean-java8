package me.rerere.matrix.util;

/** Sample retained for the reflection utility's class-literal lookup. */
public final class ReflectionGreetingSample {
    private static final long GREETING_KEY = 135636274854655L;
    private static final String GREETING_PREFIX = "Hello, ".intern();

    public ReflectionGreetingSample() { }

    private final String formatGreeting(Object[] arguments) {
        int greetingNumber = ((Integer) arguments[0]).intValue();
        long key = ((Long) arguments[1]).longValue();
        String ignoredLabel = (String) arguments[2];
        key ^= GREETING_KEY;
        long formattingKey = key ^ 129665732774241L;
        return formatGreetingArguments(new Object[] { formattingKey, greetingNumber });
    }

    private static String formatGreetingArguments(Object[] arguments) {
        long key = ((Long) arguments[0]).longValue();
        int greetingNumber = ((Integer) arguments[1]).intValue();
        key ^= GREETING_KEY;
        return new StringBuilder(GREETING_PREFIX).append(greetingNumber).toString();
    }
}
