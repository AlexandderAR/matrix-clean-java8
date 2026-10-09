package me.rerere.matrix.network;

public final class ApiResponseMessages {
    private ApiResponseMessages() {
    }

    public static String formatIndex(int index, String text) {
        return new StringBuilder("[").append(index).append("] ").append(text).toString();
    }

    public static String concatenate(String first, String second, String third, String fourth,
                                     String fifth, String sixth, String seventh, String eighth) {
        return new StringBuilder(first).append(second).append(third).append(fourth)
                .append(fifth).append(sixth).append(seventh).append(eighth).toString();
    }

    public static String wafBlocked(String rayId) {
        return new StringBuilder("You have been blocked from accessing Matrix API by WAF. Please contact Dev for more information. (Ray ID: ")
                .append(rayId).append(")").toString();
    }

    public static String matrixVersion(String version) {
        return new StringBuilder("Matrix AntiCheat ").append(version).toString();
    }

    public static String sha256Prefix(String value) {
        return new StringBuilder("sha256:").append(value).toString();
    }

    public static String tooManyRequests(String rayId) {
        return new StringBuilder("Too many requests sent to Matrix API. Please contact Dev for more information. (Ray ID: ")
                .append(rayId).append(")").toString();
    }

    public static String unexpectedResponse(int status, String rayId) {
        return new StringBuilder("Unexpected HTTPS response ").append(status).append(" (Ray ID: ")
                .append(rayId).append(")").toString();
    }

    public static String teapot(String rayId) {
        return new StringBuilder("Sorry, I'm a teapot, please try again later. (Ray ID: ")
                .append(rayId).append(")").toString();
    }
}
