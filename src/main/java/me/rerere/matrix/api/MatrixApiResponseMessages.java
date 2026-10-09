package me.rerere.matrix.api;

/** Formats the HTTP messages returned by Matrix API response handling. */
public final class MatrixApiResponseMessages {
    private MatrixApiResponseMessages() {
    }

    public static String sha256Label(String value) {
        return new StringBuilder("sha256:").append(value).toString();
    }

    public static String zeroPrefix(String value) {
        return new StringBuilder("0").append(value).toString();
    }

    public static String rateLimit(String rayId) {
        return new StringBuilder("Too many requests sent to Matrix API. Please contact Dev for more information. (Ray ID: ")
                .append(rayId).append(")").toString();
    }

    public static String teapot(String rayId) {
        return new StringBuilder("Sorry, I'm a teapot, please try again later. (Ray ID: ")
                .append(rayId).append(")").toString();
    }

    public static String brace(String value) {
        return new StringBuilder("{").append(value).append("}").toString();
    }

    public static String bracketedValue(int value, String suffix) {
        return new StringBuilder("[").append(value).append("] ").append(suffix).toString();
    }

    public static String unexpectedResponse(int responseCode, String rayId) {
        return new StringBuilder("Unexpected HTTPS response ").append(responseCode)
                .append(" (Ray ID: ").append(rayId).append(")").toString();
    }

    public static String invalidKey(String key) {
        return new StringBuilder("Invalid key: ").append(key).toString();
    }

    public static String pluginName(String name) {
        return new StringBuilder("Matrix AntiCheat ").append(name).toString();
    }

    public static String concatenate(String first, String second) {
        return new StringBuilder(first).append(second).toString();
    }

    public static String concatenate(
            String first, String second, String third, String fourth,
            String fifth, String sixth, String seventh, String eighth) {
        return new StringBuilder(first).append(second).append(third).append(fourth)
                .append(fifth).append(sixth).append(seventh).append(eighth).toString();
    }

    public static String wafBlocked(String rayId) {
        return new StringBuilder("You have been blocked from accessing Matrix API by WAF. Please contact Dev for more information. (Ray ID: ")
                .append(rayId).append(")").toString();
    }

    public static String concatenate(String first, int number, String third, String fourth) {
        return new StringBuilder(first).append(number).append(third).append(fourth).toString();
    }
}
