package me.rerere.matrix.webhook;

import java.io.IOException;
import me.rerere.matrix.thirdparty.okhttp3.Call;
import me.rerere.matrix.thirdparty.okhttp3.Callback;
import me.rerere.matrix.thirdparty.okhttp3.Response;
import me.rerere.matrix.thirdparty.okhttp3.internal.Util;

public final class DiscordWebhookCallback implements Callback {
    public DiscordWebhookCallback() {
    }

    @Override
    public void onFailure(Call call, IOException failure) {
        failure.printStackTrace();
    }

    private static String formatFailureMessage(int statusCode, String responseMessage) {
        return new StringBuilder("Discord webhook failed with code ")
                .append(statusCode).append(" and message ").append(responseMessage).toString();
    }

    private static String failureMessage(Response response) {
        int statusCode = response.code();
        String responseMessage = response.message();
        return formatFailureMessage(statusCode, responseMessage);
    }

    @Override
    public void onResponse(Call call, Response response) {
        try {
            if (!response.isSuccessful()) {
                me.rerere.matrix.Matrix.Companion.matrixInstance().getLogger().warning(
                        () -> failureMessage(response));
            }
        } finally {
            Util.closeQuietly(response);
        }
    }
}
