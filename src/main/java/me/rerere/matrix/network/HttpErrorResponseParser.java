package me.rerere.matrix.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.com.google.gson.JsonElement;
import me.rerere.matrix.thirdparty.com.google.gson.JsonObject;
import me.rerere.matrix.thirdparty.com.google.gson.JsonParser;
import me.rerere.matrix.thirdparty.kotlin.io.TextStreamsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.Charsets;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

public final class HttpErrorResponseParser {
    private HttpErrorResponseParser() {}

    public static Parsed parse(HttpsURLConnection connection, InputStream input) throws IOException {
        if (input == null) {
            throw new UnsupportedPath("sw HTTP 400 null stream is not recovered");
        }
        String body = TextStreamsKt.readText(new InputStreamReader(input, Charsets.UTF_8));
        JsonObject json = new JsonParser().parse(body).getAsJsonObject();
        JsonElement errorElement = json.get("error");
        String error = errorElement == null ? "Unknown error" : errorElement.getAsString();
        JsonElement codeElement = json.get("code");
        int code = codeElement == null ? connection.getResponseCode() : codeElement.getAsInt();

        Action action;
        if (Intrinsics.areEqual(error, "There is no valid plan for this product, you can buy one at our website")) {
            action = Action.PLAN;
        } else if (Intrinsics.areEqual(error, "Invalid token")) {
            action = Action.INVALID_TOKEN;
        } else if (StringsKt.contains(error, " ban ", true)) {
            action = Action.BAN;
        } else {
            action = Action.OTHER_UNRESOLVED;
        }
        return new Parsed(error, code, action);
    }

    public enum Action {
        PLAN, INVALID_TOKEN, BAN, OTHER_UNRESOLVED
    }

    public static final class Parsed {
        public final String error;
        public final int code;
        public final Action action;

        private Parsed(String error, int code, Action action) {
            this.error = error;
            this.code = code;
            this.action = action;
        }
    }

    public static final class UnsupportedPath extends Error {
        UnsupportedPath(String message) {
            super(message);
        }
    }
}
