package me.rerere.matrix.network.http;

/** Parsed HTTP 400 response values carried to the response handler. */
public final class ParsedBadRequest {
    final String error;
    final int code;

    ParsedBadRequest(String error, int code) {
        this.error = error;
        this.code = code;
    }

    public static ParsedBadRequest from(String error, int code) {
        return new ParsedBadRequest(error, code);
    }

    public String error() {
        return error;
    }

    public int code() {
        return code;
    }
}
